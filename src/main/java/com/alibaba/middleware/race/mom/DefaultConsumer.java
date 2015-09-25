package com.alibaba.middleware.race.mom;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.alibaba.middleware.race.mom.netty.NettyClient;

public class DefaultConsumer implements Consumer{
	private static final Logger LOG = Logger.getLogger(DefaultConsumer.class.getCanonicalName());
	
	private NettyClient nettyClient;
	private String gid;
	private String topic;
	private String filterKey;
	private String filterValue;
	private MessageListener listener;
	private ExecutorService executor;
	
	private boolean isRunning = false;
	
	public DefaultConsumer(){
		try {
//			LOG shutdown
			LogManager.getLogManager().reset();
//			String brokerIp=System.getProperty("SIP");
			String brokerIp = System.getProperty("SIP", "localhost");
			nettyClient = new NettyClient(brokerIp, BrokerEngine.PORT, this);
			executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
		} catch (Exception e) {
			LOG.severe(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void onMessageReceived(Message msg){
		executor.submit(new MessageConsumer(msg));
	}
	
	@Override
	public void subscribe(String topic, String filter, MessageListener listener) {
		if(isRunning)
			throw new IllegalStateException("Consumer already running");
		if(topic == null || listener == null)
			throw new NullPointerException();
		if(filter != null && !filter.equals("")){
			String[] spl = filter.split("=");
			if(spl.length != 2)
				throw new IllegalArgumentException("wrong filter format");
			filterKey = spl[0];
			filterValue = spl[1];
		}
		
		this.topic = topic;
		this.listener = listener;
	}

	@Override
	public void setGroupId(String groupId) {
		if(isRunning)
			throw new IllegalStateException("Consumer already running");
		if(groupId == null)
			throw new NullPointerException();
		this.gid = groupId;
	}

	@Override
	public void start(){
		if(isRunning)
			throw new IllegalStateException("Consumer already running");
		if(gid == null || topic == null)
			throw new IllegalStateException("groupId or topic not set yet");
		
		try {
			nettyClient.start();
			LOG.info("send reg ctrlMsgs");
			nettyClient.sendMessage(ControlMessage.newRegisterMessage(gid, false));
			nettyClient.sendMessage(
					ControlMessage.newSubscribeMessage(topic, filterKey, filterValue));
		} catch (Exception e) {
			LOG.severe(e.getMessage());
			throw new RuntimeException(e);
		}
		
		isRunning = true;
	}

	@Override
	public void stop() {
		if(!isRunning)
			throw new IllegalStateException("Consumer not running");
		isRunning = false;
		try {
			nettyClient.sendMessage(ControlMessage.newUnsubscribeMessage(topic));
			nettyClient.sendMessage(ControlMessage.newUnregisterMessage(false));
			nettyClient.stop();
		} catch (Exception e) {
			LOG.severe(e.getMessage());
			throw new RuntimeException(e);
		}
		
		LOG.info("Consumer stopped");
	}
	
	class MessageConsumer implements Runnable{
		private Message msg;
		public MessageConsumer(Message msg) {
			this.msg = msg;
		}
		@Override
		public void run() {
			listener.onMessage(msg);
			ControlMessage ctrlMsg = ControlMessage.newAckMSGMessage(msg.getMsgId() , msg.getBornTime());
			nettyClient.sendMessageAsync(ctrlMsg, null);
		}
	}
}
