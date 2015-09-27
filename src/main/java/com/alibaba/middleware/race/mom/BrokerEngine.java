package com.alibaba.middleware.race.mom;

import io.netty.channel.Channel;
import io.netty.util.internal.ConcurrentSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.alibaba.middleware.race.mom.netty.NettyServer;
import com.alibaba.middleware.race.mom.store.FileEngine;
import com.alibaba.middleware.race.mom.store.QueueManager;

/**
 * 整个Broker服务器的启动入口类。
 * 
 * @author Zhangkefei
 *
 */
public class BrokerEngine {
	private static final Logger LOG = Logger.getLogger(BrokerEngine.class.getCanonicalName());
	public static final int PORT = 9999;
	private static final BrokerEngine BROKER_ENGINE = new BrokerEngine();
	public static BrokerEngine getInstance(){
		return BROKER_ENGINE;
	}
	
	private volatile boolean isRunning = false;
	private FileEngine fileEngine;
	private QueueManager queueManager;
	private ExecutorService msgExecutor;
	private ExecutorService storeExecutor;
	private NettyServer nettyServer;
	private Set<Channel> producerChannels;
	
//	Net relation
	
	// consumer GroupID <--> Channels set
	private Map<Channel, String> channelGroupIdMap;
	private Map<String, Set<Channel>> groupIdChannelsMap;
	
//	Subscribe relation
	
	private Set<String> groupIDList;
	
	// consumer FilterStr (TopicID + FilterKey + FilterValue) <-- GroupID
	private Map<String, Set<String>> groupIDFilterStrMap;
	// consumer FilterKey <-- GroupID
	private Map<String, Set<String>> groupIDFilterKeyMap;
	// consumer FilterKeyStr (TopicID + FilterKey) <-- GroupID
	private Map<String, Set<String>> groupIDFilterKeyStrMap;
	
	// Msg relationship topic -> GroupID
	private Map<String, Set<String>> topicGroupIdsMap;
	private Map<String, String> groupIDTopicMap;
	
	// String builder for filterStr
	StringBuilder sb;
	
//	Accumulate Msg's MsgID
	private Queue<String> accumulateQueue;
	 
	private ReSendControler reSendControler;
	
//	private Map<Integer , FileEngine> fileEngineList;
	
	public Map<String, Set<String>> getTopicGroupIdsMap() {
		return topicGroupIdsMap;
	}

	private BrokerEngine(){
		//TODO
		msgExecutor = Executors.newFixedThreadPool(
				Runtime.getRuntime().availableProcessors() * 2);
		storeExecutor = Executors.newSingleThreadExecutor();
//		storeExecutor = Executors.newFixedThreadPool(
//				3);
//		
		nettyServer = new NettyServer(PORT);
		
//		int StoreThreadNum = 3;
//		storeExecutor = Executors.newFixedThreadPool(StoreThreadNum);
//		fileEngineList = new ConcurrentHashMap<Integer, FileEngine>();
//		queueManager = new QueueManager(this);
//		for(int i=0 ; i<StoreThreadNum ; i++){
//			try {
//				fileEngine = new FileEngine(queueManager , i);
//				fileEngineList.put(i , fileEngine);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		try {
			queueManager = new QueueManager(this);
			fileEngine = new FileEngine(queueManager);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		nettyServer = new NettyServer(PORT);
		
		
		producerChannels = Collections.synchronizedSet(new HashSet<Channel>());
		
		channelGroupIdMap = new ConcurrentHashMap<Channel, String>();
		groupIdChannelsMap = new ConcurrentHashMap<String, Set<Channel>>();
		
		groupIDList = Collections.synchronizedSet(new HashSet<String>());
		
		topicGroupIdsMap = new ConcurrentHashMap<String, Set<String>>();
		groupIDTopicMap = new ConcurrentHashMap<String, String>();
		
		groupIDFilterStrMap = new ConcurrentHashMap<String, Set<String>>();
		groupIDFilterKeyMap = new ConcurrentHashMap<String, Set<String>>();
		groupIDFilterKeyStrMap = new ConcurrentHashMap<String, Set<String>>();
		
		accumulateQueue = new ConcurrentLinkedQueue<String>();;
		
//		恢复持久化的订阅关系
		try {
			recoverySubscribeRelation(accumulateQueue);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		恢复堆积的包
		try {
			recoveryAccumulateMsg();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		启动 重传 线程
//		Executor executor = anExecutor;
		ReSendHandler reSendHandler = new ReSendHandler();
		reSendHandler.setSleepDelay(1000);
		reSendControler = new ReSendControler(1 , reSendHandler);
	}
	
	/** 恢复 堆积消息 的MsgID
	 * @throws IOException
	 */
	private void recoveryAccumulateMsg() throws IOException {
		LOG.info("fileEngine read subscribe relation");
		fileEngine.readerAccumulateMsgQueue(accumulateQueue);
		
	}

	/** 恢复 订阅关系
	 * 
	 * @param accumulateQueue 
	 * 
	 */
	private void recoverySubscribeRelation(Queue<String> accumulateQueue) throws IOException {
		LOG.info("fileEngine recovery subscribe relation");
		fileEngine.readerSubscribe(groupIDList , groupIDTopicMap , groupIDFilterStrMap , groupIDFilterKeyMap , groupIDFilterKeyStrMap);
		// recovery topicGroupIdsMap
		for(Map.Entry<String, String> entry: groupIDTopicMap.entrySet()){
			Set<String> groupIds = topicGroupIdsMap.get(entry.getValue());
			if(groupIds == null){
				groupIds = Collections.synchronizedSet(new HashSet<String>());
				topicGroupIdsMap.put(entry.getValue(), groupIds);
			}
			groupIds.add(entry.getKey());
		}
	}
	
	/** is there is a accumulate msg
	 * @return
	 */
	public boolean isAccumulateEmpty(){
		return accumulateQueue.isEmpty();
	}
	
	/**重传调用 
	 * 读取文件中 msg 然后发送
	 * 
	 */
	public void onSendAccumulateMsg() throws IOException{
		String msgID = accumulateQueue.poll();
		LOG.info("fileEngine read msg ,msgID:" + msgID);
		Message msg = fileEngine.readerByMsgID(msgID);
		msgExecutor.submit(new MessageSender(msg));
	}

	/** consumer first connect to broker
	 *  register
	 *  create groupID <--> channel 
	 * @param channel
	 * @param groupId
	 * @param isProducer
	 * @throws CtrlMsgException
	 */
	private void channelRegister(Channel channel, String groupId, boolean isProducer) 
			throws CtrlMsgException{
		LOG.info("channel reg, groupId:" + groupId);
		if(isProducer){
			producerChannels.add(channel);
			return;
		}
		
		// consumer channel
		if(!groupIDList.contains(groupId)){
			// 第一次见这个groupID
			groupIDList.add(groupId);
		}
		if(channelGroupIdMap.containsKey(channel))
			throw new CtrlMsgException("channel already registered.");
		
		channelGroupIdMap.put(channel, groupId);
		Set<Channel> chSet = groupIdChannelsMap.get(groupId);
		if(chSet == null){
			chSet = Collections.synchronizedSet(new HashSet<Channel>());
			groupIdChannelsMap.put(groupId, chSet);
		}
		chSet.add(channel);
	}
	
	/** 一个consumer断掉了，那个channel就会移除
	 * 
	 * @param channel
	 * @param isProducer
	 * @throws CtrlMsgException
	 */
	public void channelUnregister(Channel channel) 
			throws CtrlMsgException{
		LOG.info("channel unreg");
//		if(isProducer){
//			producerChannels.remove(channel);
//			return;
//		}
		
		String gid = channelGroupIdMap.remove(channel);
		if(gid == null)
			throw new CtrlMsgException("channel not registered, but sent unregister msg.");
		
		Set<Channel> chSet = groupIdChannelsMap.get(gid);
		chSet.remove(channel);
	}
	
	/** consumer subscribe a kind of msg
	 *  add a subscribe relation for a groupID
	 *  
	 * @param channel
	 * @param topic
	 * @param filterKey
	 * @param filterValue
	 * @throws CtrlMsgException
	 */
	private void subscribeTopic(Channel channel, 
			String topic, String filterKey, String filterValue) throws CtrlMsgException{
		LOG.info("subscribe topic: " + topic);
		
		String gid = channelGroupIdMap.get(channel);
		if(gid == null)
			throw new CtrlMsgException("group not set yet.");
		
		// groupID <--> Topic
		Set<String> groupIds = topicGroupIdsMap.get(topic);
		if(groupIds == null){
			groupIds = Collections.synchronizedSet(new HashSet<String>());
			topicGroupIdsMap.put(topic, groupIds);
		}
		groupIds.add(gid);
		
		if(!groupIDTopicMap.containsKey(gid)){
			groupIDTopicMap.put(gid, topic);
		}
		
		// Filter
		String filterStr = null;
		String filterKeyStr = null;
		if(filterKey!=null && filterValue!=null){
			// 1 filter present as filterStr (filterKey + filterValue) 
			sb = new StringBuilder(topic);
			sb.append(filterKey);
			sb.append(filterValue);
			filterStr = sb.toString();
			
			Set<String> filterStrs = groupIDFilterStrMap.get(gid);
			if(filterStrs == null){
				filterStrs = Collections.synchronizedSet(new HashSet<String>());
				groupIDFilterStrMap.put(gid, filterStrs);
			}
			filterStrs.add(sb.toString());
			
			// 2 filterKey map
			Set<String> filterKeys = groupIDFilterKeyMap.get(gid);
			if(filterKeys == null){
				filterKeys = Collections.synchronizedSet(new HashSet<String>());
				groupIDFilterKeyMap.put(gid, filterKeys);
			}
			filterKeys.add(filterKey);
			
			// 3 filterKeyStr map
			Set<String> filterKeyStrs = groupIDFilterKeyStrMap.get(gid);
			if(filterKeyStrs == null){
				filterKeyStrs = Collections.synchronizedSet(new HashSet<String>());
				groupIDFilterKeyStrMap.put(gid, filterKeyStrs);
			}
			sb = new StringBuilder(topic);
			sb.append(filterKey);
			filterKeyStr = sb.toString();
			filterKeyStrs.add(sb.toString());
		}
		
		// write into fileSystem
//		fileEngine.writeRelation(gid , topic , filterStr , filterKey , filterKeyStr);
	}
	
	private void unsubscribeTopic(Channel channel, String topic) 
			throws CtrlMsgException{
		
//		TODO: not yet complete
		
//		Map<Channel, Filter> channelFiltersMap = topicChannelsMap.get(topic);
//		if(channelFiltersMap == null)
//			throw new CtrlMsgException("no topic registered");
//		
//		channelFiltersMap.remove(channel);
	}
	
	// consumer sendback reciveSucc ack
	// notify the fileEngine , msg send succ
	private void ackMessage(Channel channel, ControlMessage msg){
		if(msg.isSucceeded()){
			long currentTime = System.currentTimeMillis();
			long delay = currentTime - msg.getOriginalMsgBornTime();
			
			// 收到ACK包 为延迟10s内
			if(delay < 10000){
				String msgGroupID = channelGroupIdMap.get(channel);
				accumulateQueue.remove(msg.getMsgId());
				// fileEngine record ack Msg
//				fileEngine.onMessageConsumed(msg.getMsgId() , groupIDTopicMap.get(msgGroupID), msgGroupID, true);
				return;
			}
		}
		
		//TODO
	}
	
	public boolean isProducer(Channel channel){
		return producerChannels.contains(channel);
	}
	
	public void onCtrlMsgReceived(ControlMessage msg, Channel channel){
		LOG.info("CtrlMsgReceived, id: " + msg.getMsgId());
		try{
			switch (msg.getCtrlType()) {
			case REG:
				channelRegister(channel, msg.getGroupId(), msg.isProducer());
				break;
			case UNREG:
//				TODO:
//				channelUnregister(channel, msg.isProducer());
				break;
			case SUB:
				subscribeTopic(channel, msg.getTopic(), msg.getFilterKey(), msg.getFilterValue());
				break;
			case UNSUB:
				unsubscribeTopic(channel, msg.getTopic());
				break;
			case ACK:
//				ackMessage(channel, msg);
				return;		// no need to ack an ack msg;
			case ACK_MSG:
				ackMessage(channel, msg);
				return;		// no need to ack an ack msg;
				
			default:
				break;
			}
		} catch(CtrlMsgException e){
			channel.writeAndFlush(ControlMessage.newFailMessage(e, msg.getMsgId()));
			return;
		}
		
		channel.writeAndFlush(ControlMessage.newAckMessage(msg.getMsgId()));
	}

	/**
	 * NettyServer接收到Poducer发来的message后调用此回调函数。
	 * 
	 * @param msg
	 * @param channel
	 */
	public void onMessageReceived(Message msg, Channel channel){
		LOG.info("get Message! id: " + msg.getMsgId());
//		storeExecutor.submit(new MessageStore(fileEngine , this.queueManager ,  msg , channel));
		
		// fileEngine register sendMsg's topic
		// file store in msgr
//		try {
//			queueManager.register(msg.getTopic());
//			fileEngine.writer(msg, channel);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
////		Test with no fileSystem
//		onMessageWritten(msg , channel);
		
		//TODO below is for test purpose
//		call back of store success , move in the fileEngine
		onMessageWritten(msg, channel);
	}
	
	/**
	 * FileEngine完成该message的持久化存储后调用该回调函数，此函数非阻塞，可以立即返回。
	 * 
	 * @param msg
	 * @param channel
	 */
	public void onMessageWritten(Message msg, Channel channel){
		
		msgExecutor.submit(new ProducorACKSender(channel , msg));
		
		
		//TODO below is for test purpose
		//TODO not yet compelet
		Set<String> groupIDs = topicGroupIdsMap.get(msg.getTopic());
		for(String groupID_test : groupIDs){
			LOG.info("fileEngine read msg , msgTopic:" + msg.getTopic() + " groupID:" + groupID_test);
//				fileEngine.reader(msg.getTopic(), groupID_test);

//			try {
//				LOG.info("fileEngine read msg , msgTopic:" + msg.getTopic() + " groupID:" + groupID_test);
//				fileEngine.reader(msg.getTopic(), groupID_test);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

			msgExecutor.submit(new MessageSender(msg));
		}
	}
	
	/**
	 * 开始整个Broker服务器
	 * 
	 * @throws InterruptedException
	 */
	public void start() throws InterruptedException{
		if(isRunning) return;
    	synchronized (this) {
    		if(isRunning) return;
    		isRunning = true;
    		
//    		fileEngine.start();
    		nettyServer.start();
    	}
	}
	
	/**
	 * 停止整个Broker服务器
	 * 
	 */
	public void stop(){
		if(! isRunning) return;
    	
    	synchronized (this) {
    		if(! isRunning) return;
    		isRunning = false;
    		
			nettyServer.stop();
			msgExecutor.shutdown();
			fileEngine.stop();
    	}
		
		//TODO
	}
	
	public void onChannelCloseAbnormally(Channel channel){
		//TODO
	}
	
	
	/**	MessageSender 
	 * 
	 * @author zkf
	 *
	 */
	class MessageSender implements Runnable{
		private Message msg;
		private Set<String> filterStr;
		public MessageSender(Message msg) {
			this.msg = msg;
		}
		
		/** 判断这个msg是不是该这个groupID收
		 * @param groupID
		 * @param msg
		 * @return
		 */
		private boolean filter(String groupID, Message msg) {
			// TODO:
			// 先找该channel订阅的所有Key ， 再通过Key去找是否匹配
			Set<String> msgKeySet = groupIDFilterKeyMap.get(groupID);
			if (msgKeySet == null)
				return true;
			for (String msgKey : msgKeySet) {
				if (msg.getProperty(msgKey) != null) {
					//
					sb = new StringBuilder(msg.getMsgId());
					sb.append(msgKey);
					// 判定是否为 默认空 filter
					if (!groupIDFilterKeyStrMap.get(groupID).contains(
							sb.toString()))
						return true;
					// 判定 属性值是否一致
					sb.append(msg.getProperty(msgKey));
					if (!groupIDFilterStrMap.get(groupID).contains(
							sb.toString()))
						return true;
				}
			}
			return false;
		}
		
		@Override
		public void run() {
			String topic = msg.getTopic();
			LOG.info("sending msg id : " + msg.getMsgId() + " on topic: " + topic);
			Set<String> groupSet = topicGroupIdsMap.get(topic);
			for(String gid: groupSet){
				if(filter(gid , msg)){
					// 这个GroupID 是 要收这个msg的
					Set<Channel> chSet = groupIdChannelsMap.get(gid);
					for(Channel channel: chSet){
						//属于这个groupID的channel ， 发送一个就好
						channel.writeAndFlush(msg);
					}
				}
			}
		}
	}
	
	class ProducorACKSender implements Runnable{
		Channel channel;
		Message msg;
		
		public ProducorACKSender(Channel channel , Message msg) {
			// TODO Auto-generated constructor stub
			this.channel = channel;
			this.msg = msg;
		}
		
		@Override
		public void run() {
			channel.writeAndFlush(ControlMessage.newAckMessage(msg.getMsgId()));
		}
	}
	
	/* 存储Msg 线程
	 * @author fsc
	 *
	 */
	class MessageStore implements Runnable{
		private FileEngine fileEngine;
		private QueueManager queueManager;
		private Message msg;
		private Channel channel;
		public MessageStore(FileEngine fileEngine , QueueManager queueManager ,  Message msg , Channel channel ) {
			this.fileEngine = fileEngine;
			this.queueManager = queueManager;
			this.msg = msg;
			this.channel = channel;
		}
		
		@Override
		public void run() {
			queueManager.register(msg.getTopic());
			try {
				fileEngine.writer(msg, channel);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
//		LogManager.getLogManager().reset();	//TODO: temp
		try {
			BrokerEngine.getInstance().start();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
