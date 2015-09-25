package com.alibaba.middleware.race.mom.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.alibaba.middleware.race.mom.BrokerEngine;
import com.alibaba.middleware.race.mom.Message;
import com.alibaba.middleware.race.mom.store.util.BufferedRandomAccessFile;
import com.alibaba.middleware.race.mom.store.util.KryoTool;

import io.netty.channel.Channel;
/**
 * FileEngine 
 * 1, 负责把当前messageIndex保存在相应的消息队列中，并把Message持久化保存在文件系统中
 * 2, 从文件系统中读取相应的消息
 * @author wh
 *
 */
public class FileEngine {
	
	/**
	 * 存储单元的大小
	 * 注：offset*20才是message的偏移量，length是存储了几个单元
	 */
	
	public static int storeUnitSize;
	private KryoTool kryoTool;
	private volatile boolean isRunning;
	private String filePath;
	private RandomAccessFile randomAccessFile;
	private FileChannel fileChannel;
	private ByteBuffer mappedByteBuffer;
	private int fileIndex;
	private QueueManager queueManager;
	private long fileWritePosition;
	//1M
	private static final int FILE_MAX_LENGTH = 0x100000;
	private static final int DEFAULT_File_NUMBER = 5;
	
	private Map<Message , Channel> commitMessageList;
	
	private final int MAX_BUFFER_LENGTH = 0x1000000;
	private final int MAX_MESSAGRE_LENGTH = 100;
	private int cur_message_length = 0;
//	private final String baseFilePath;
	
	// commit service
	private final ScheduleCommitService scheduleCommitService;
	
	// appendMsg Executor
//	private ExecutorService appendMsgExecutor;

//	// 读写锁
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    //测试一定时间间隔内有没有收到message, 如果没再收到就刷盘
	private final int interval = 50;
	
    
    // isMsgReceived
    private volatile long lastRecivedTimeStamp = 0L;
    
    private volatile boolean isShouldFlush = false;
	private volatile ByteBuffer byteBuffer;
   
	//LOG
	private static final Logger LOG = Logger.getLogger(BrokerEngine.class.getCanonicalName());
	
	// test
	public static long totalDelay = 0L;
	
	public FileEngine(QueueManager queueManager) throws IOException{

		this.storeUnitSize = 20;
		//当前读写的文件序号
		this.fileIndex = 0;
        //$userhome
		String userhome = System.getProperty("user.home");
		//linux
//		this.baseFilePath = userhome + "/store/messageLog_";
//		this.filePath = this.baseFilePath + this.fileIndex;
		this.filePath = userhome + "/store/messageLog";
		

		this.queueManager = queueManager;
		this.isRunning = false;
		this.kryoTool = KryoTool.getInstance();
//		this.randomAccessFile = new BufferedRandomAccessFile(filePath, "rw", 10);
////		this.randomAccessFile = new RandomAccessFile(filePath, "rw");
//		
//		this.fileChannel = new RandomAccessFile(filePath, "rw").getChannel();
//		this.fileWritePosition = 0;
	
		this.queueManager = queueManager;
		this.commitMessageList = new ConcurrentHashMap<Message, Channel>();
		
        //注意，文件通道的可读可写要建立在文件流本身可读写的基础之上  
//        this.mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_BUFFER_LENGTH); 
        
		try {
			File file = new File(filePath);
			this.randomAccessFile = new RandomAccessFile(file, "rw");
			this.fileChannel = this.randomAccessFile.getChannel();
//			this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_BUFFER_LENGTH); 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// 占位
		this.mappedByteBuffer = getRandombyte();
		this.mappedByteBuffer.limit(100*1024*1024);
		
		this.byteBuffer = ByteBuffer.allocate(16*1024);
		this.fileChannel.write(mappedByteBuffer);
		this.fileChannel.force(true);
		this.fileChannel.position(0);
        //commit service
		this.scheduleCommitService = new ScheduleCommitService(mappedByteBuffer , readWriteLock , this);
        this.scheduleCommitService.start();
        
       }
	
	public ByteBuffer getRandombyte() {
		ByteBuffer bytebuffer = ByteBuffer.allocate(100 * 1024 *1024);
		byte[] bb=new byte[100 * 1024 *1024];  
		bytebuffer.put(bb);
		  return bytebuffer;
		 }
	/**
	 * 启动FileEngine
	 */
	public void start(){
		
	}
	
	/**
	 * 停止FileEngine
	 */
	public void stop(){
		
	}
	
	/**
	 * 把当前messageIndex保存在相应的消息队列中，并把Message持久化保存在文件系统中
	 * 通过topic获取messageGroup
	 * 通过queueId获取messageQueue
	 * @param message
	 * @param queueId
	 * @throws IOException 
	 */
	public void writer(Message message, Channel channel) throws IOException{
		
		this.lastRecivedTimeStamp = System.currentTimeMillis();
		
		LOG.info("fileEngine writer msgID:"+message.getMsgId());
		
//		System.out.println("msg id : " + message.getMsgId() + ", delay : " + (System.currentTimeMillis() - message.getBornTime()));
		
		String msgId = message.getMsgId();
		String topic = message.getTopic();
		byte[] messageByte = kryoTool.encode(message);
		
		/**
		 * 修改该topic下QueueGroup的消费进度Map
		 */
		QueueGroup queueGroup = queueManager.getQueueGroup(topic);
		queueGroup.adapt(message);
		
		/**
		 * 将message保存在物理文件中，做持久化存储
		 * 每次都将message插入到文件末尾
//		 */
//		if(this.fileWritePosition > FILE_MAX_LENGTH) {
//			
//			fileIndex ++;
//			
//			createNextFile(fileIndex);
//		}
		
		long offset = this.getNextOffset();
		long length = messageByte.length;
		this.fileWritePosition += length;
		
		/**
		 * 将索引插入队列中
		 */
		//生成文件索引
		MessageIndex messageIndex = new MessageIndex(msgId, offset, length);
		
		//选取合适的queue来插入messageIndex
		long queueId = Router.route(message);
		MessageQueue messageQueue = queueManager.getMessageQueue(topic, queueId);
		//将messageIndex保存到队列中
		messageQueue.add(messageIndex);
		
//		mappedByteBuffer.put(messageByte);
//		mappedByteBuffer.force();
		
//		this.mappedByteBuffer.put(messageByte);
//		this.mappedByteBuffer.force();
//		BrokerEngine.getInstance().onMessageWritten(message, channel);
		
//		appendMsgExecutor.submit(new AppendService(message , messageByte , this));
		
		this.appendMessage(message , messageByte , channel);
		
		this.commitMessage();
		
		// commit 
//		CommitRequest commitRequest = new CommitRequest(message, channel);
//		
//		readWriteLock.writeLock().lock();
//		this.commitService.putRequest(commitRequest);
//		readWriteLock.writeLock().unlock();
//		
//		boolean flushOK = commitRequest.waitForFlush(1000 * 5);
//		if (!flushOK) {
////            log.error("do groupcommit, wait for flush failed, topic: " + msg.getTopic() + " tags: "
////                    + msg.getTags() + " client address: " + msg.getBornHostString());
//			System.out.println("LOG commit flush TIMEOUT");
//        }
//		else{
//			BrokerEngine.getInstance().onMessageWritten(message, channel);
//		}
		
	}
	
	/**
	 * 从文件系统中读取相应的消息
	 * 返回groupId订阅的topic中没有被消费的message
	 * @param msgId
	 * @throws IOException 
	 */
	public Message reader(String topic, String groupId) throws IOException{
		
		//通过topic和groupId获得messageIndex
		QueueGroup queueGroup = queueManager.getQueueGroup(topic);
		String msgId = queueGroup.getAvalibleMessageId(groupId);
		MessageIndex messageIndex = queueGroup.getMessageIndex(msgId);
		
		//获得该message在物理文件中的偏移和长度
		long offset = messageIndex.getOffset();
		long length = messageIndex.getLength();
			
		//从物理文件中读取message
		randomAccessFile.seek(offset);
		
		byte[] buff=new byte[(int) length];
		randomAccessFile.read(buff);
		
		Message message = kryoTool.decode(buff);
		
		return message;
	}
	
	/**
	 * 获取文件系统的下一个偏移位置
	 * @return
	 * @throws IOException 
	 */
	public long getNextOffset() throws IOException{
		/**
		 * 暂时每次都在文件末尾添加message记录
		 */
		
//		return fileWritePosition;
		return randomAccessFile.length();
		
//		return 0;
	}
	
	/**
	 * 向ByteBuffer追加消息
	 * @param data
	 */
	public void appendMessage(Message message, byte[] data , Channel channel) {
		
		byteBuffer.put(data);
        this.cur_message_length += 1;
        this.commitMessageList.put(message , channel);
        
    }
	
	private void writeFile() {
		
		long start = System.currentTimeMillis();
		
		try {
			this.fileChannel.write(this.byteBuffer);
			this.byteBuffer.flip();
			this.fileChannel.force(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(System.currentTimeMillis() - start);
		
	}
	
	public void commitMessage(){
		
		if((cur_message_length > 0) && (isShouldFlush() || (cur_message_length >= MAX_MESSAGRE_LENGTH))){
			
			writeFile();
			
			//callback
			for(Map.Entry<Message , Channel> msgChannelSet : this.commitMessageList.entrySet()){
				BrokerEngine.getInstance().onMessageWritten(msgChannelSet.getKey(), msgChannelSet.getValue());
			}
			
			this.cur_message_length = 0;
			this.commitMessageList.clear();
			
			setShouldFlush(false);
			setLastRecivedTimeStamp(System.currentTimeMillis());
		}
		
	}

	/**
	 * 非阻塞回调,设置该message已被该group消费
	 * @param message
	 * @param groupId
	 * @param consumeResult
	 */
	public void onMessageConsumed(String msgId, String topic ,String groupId, boolean consumeResult){
//		String msgId = message.getMsgId();
//		String topic = message.getTopic();
		
		QueueGroup queueGroup = queueManager.getQueueGroup(topic);
		
		queueGroup.setMsgIdMap(msgId, groupId, consumeResult);
	}
	
	/**
	 * 采取随机方式选取下一个MessageIndex
	 * @param messageQueue
	 * @return
	 */
	public MessageIndex getNextMessageIndex(MessageQueue messageQueue){
		Random random = new Random();
        int nextIndex = random.nextInt(messageQueue.length());
        
        return messageQueue.get(nextIndex);
	}
	
	public static void main(String[] args) throws IOException{
//		QueueManager queueManager = new QueueManager();
//		FileEngine fileEngine = new FileEngine(queueManager);
//	
//		String topic = "test file engine";
//		queueManager.register(topic);
//		
//		Message message = new Message();
//		message.setTopic(topic);
//		
//		fileEngine.writer(message);
//		
//		Message m = fileEngine.reader(topic);
//		
//		System.out.println( m.getTopic() );
		
		
	}

	/** 通过MsgID读取Msg
	 * @param msgID
	 * @return
	 */
	public Message readerByMsgID(String msgID) {
		// TODO Auto-generated method stub
//		TODO
		return null;
	}

	/** 恢复订阅关系
	 * @param groupIDList 
	 * @param groupIDTopicMap
	 * @param groupIDFilterStrMap
	 * @param groupIDFilterKeyMap
	 * @param groupIDFilterKeyStrMap
	 */
	public void readerSubscribe(Set<String> groupIDList, Map<String, String> groupIDTopicMap,
			Map<String, Set<String>> groupIDFilterStrMap,
			Map<String, Set<String>> groupIDFilterKeyMap,
			Map<String, Set<String>> groupIDFilterKeyStrMap) {
		// TODO Auto-generated method stub
		
	}

	/** 恢复堆积消息队列
	 * @param accumulateQueue
	 */
	public void readerAccumulateMsgQueue(Queue<String> accumulateQueue) {
		// TODO Auto-generated method stub
		
	}

	/** 每次订阅时写入的订阅关系
	 * groupID -> topic
	 * groupID -> filterStr
	 * groupID -> filterKey
	 * groupID -> filterKeyStr
	 * @param gid
	 * @param topic
	 * @param filterStr
	 * @param filterKey
	 * @param filterKeyStr
	 */
	public void writeRelation(String gid, String topic, String filterStr,
			String filterKey, String filterKeyStr) {
		// TODO Auto-generated method stub
		
	}
	 
	public long getLastRecivedTimeStamp() {
		return lastRecivedTimeStamp;
	}
	
	public void setLastRecivedTimeStamp(long lastRecivedTimeStamp) {
		this.lastRecivedTimeStamp = lastRecivedTimeStamp;
	}
	
	public boolean isShouldFlush() {
		return isShouldFlush;
	}


	public void setShouldFlush(boolean isShouldFlush) {
		this.isShouldFlush = isShouldFlush;
	}

}

class CommitRequest {
    private final Message msg;
    private final Channel channel;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private volatile boolean flushOK = false;


    public CommitRequest(Message msg , Channel channel) {
    	this.msg = msg;
    	this.channel = channel;
    }

    public void wakeupCustomer(final boolean flushOK) {
        this.flushOK = flushOK;
        this.countDownLatch.countDown();
    }


    public boolean waitForFlush(long timeout) {
        try {
            boolean result = this.countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
            return result || this.flushOK;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}

class CommitService implements Runnable{

	MappedByteBuffer mappedByteBuffer;
	protected final Thread thread;
	protected volatile boolean stoped = false;
	private volatile List<CommitRequest> commitRequestQueue_write = new ArrayList<CommitRequest>();
	private volatile List<CommitRequest> commitRequestQueue = new ArrayList<CommitRequest>();
	private volatile boolean hasNotified = false;
	// 读写锁
    private final ReadWriteLock readWriteLock;
	
	
	public CommitService(MappedByteBuffer mappedByteBuffer , ReadWriteLock lock) {
		// TODO Auto-generated constructor stub
		this.thread = new Thread(this, "commitService");
		this.mappedByteBuffer = mappedByteBuffer;
		this.readWriteLock = lock;
	}
	
	 public void putRequest(final CommitRequest request) {
		 
		 synchronized (this) {
			 this.commitRequestQueue.add(request);
             if (!this.hasNotified) {
                 this.hasNotified = true;
                 this.notify();
             }
         }
     }

	  private void swapRequests() {
          List<CommitRequest> tmp = this.commitRequestQueue_write;
          this.commitRequestQueue_write = this.commitRequestQueue;
          this.commitRequestQueue = tmp;
      }

	 
	public void start() {
		this.thread.start();
	}
	
	public void stop() {
		  // TODO
//	        this.stop(false);
	}
	  
	public boolean isStoped() {
		return stoped;
	}	
	
	protected void waitForRunning(long interval) {
        synchronized (this) {
            if (this.hasNotified) {
                this.hasNotified = false;
                this.swapRequests();
                return;
            }

            try {
                this.wait(interval);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                this.hasNotified = false;
                this.swapRequests();
            }
        }
    }
	
	 public void wakeup() {
	        synchronized (this) {
	            if (!this.hasNotified) {
	                this.hasNotified = true;
	                this.notify();
	            }
	        }
	    }
	

	private void doCommit() {
			// TODO Auto-generated method stub
		if(!this.commitRequestQueue.isEmpty()){
			
			readWriteLock.writeLock().lock();
			
			this.mappedByteBuffer.force();
			
			System.out.println("LOG Length of Request :" + commitRequestQueue.size());
			
			for(CommitRequest commitRequest : commitRequestQueue){
				commitRequest.wakeupCustomer(true);
			}
			commitRequestQueue.clear();
			readWriteLock.writeLock().unlock();
			
		}
	}
	 
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!stoped /* isNotifed */ ){
			this.waitForRunning(0);
			
			
				
				this.doCommit();
				
//				try {
//					thread.sleep(10);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
			}
				try {
					thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
	}
}


//class AppendService implements Runnable{
//
//	private final Message message;
//	private final byte[] messageByte;
//	private final FileEngine fileEngine;
//	
//	public AppendService(Message message, byte[] messageByte,
//			FileEngine fileEngine) {
//		// TODO Auto-generated constructor stub
//		this.message = message;
//		this.messageByte = messageByte;
//		this.fileEngine = fileEngine;
//	}
//
//	@Override
//	public void run() {
//		// TODO Auto-generated method stub
//		this.fileEngine.appendMessage(message, messageByte);
//	}
//	
//}

class ScheduleCommitService implements Runnable{
	private final ByteBuffer mappedByteBuffer;
	private final ReadWriteLock readWriteLock;
	private final FileEngine fileEngine;
	
	protected final Thread thread;
	protected volatile boolean stoped = false;
	
	public ScheduleCommitService(ByteBuffer mappedByteBuffer , ReadWriteLock lock , FileEngine fileEngine) {
		// TODO Auto-generated constructor stub
		this.mappedByteBuffer = mappedByteBuffer;
		this.readWriteLock = lock;
		this.fileEngine = fileEngine;
		this.thread = new Thread(this, "commitService");
	}
	
	public void start() {
		this.thread.start();
	}
	
	public void stop() {
		  // TODO
//	        this.stop(false);
	}
	  
	public boolean isStoped() {
		return stoped;
	}	
	
	private void doCommit() {
		Buffer commitRequestQueue;
	// TODO Auto-generated method stub
		
		readWriteLock.writeLock().lock();
		
//		this.mappedByteBuffer.force();
		
		readWriteLock.writeLock().unlock();
		
}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!isStoped()){
			
			if(System.currentTimeMillis() - this.fileEngine.getLastRecivedTimeStamp() > 200){
//				doCommit();
				this.fileEngine.setLastRecivedTimeStamp(System.currentTimeMillis());
				this.fileEngine.setShouldFlush(true);
				this.fileEngine.commitMessage();
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
}


