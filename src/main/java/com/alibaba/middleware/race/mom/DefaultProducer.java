package com.alibaba.middleware.race.mom;

import java.util.concurrent.ExecutionException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.alibaba.middleware.race.mom.async.MessageFuture;
import com.alibaba.middleware.race.mom.netty.NettyClient;

public class DefaultProducer implements Producer{
	private static final Logger LOG = Logger.getLogger(DefaultProducer.class.getCanonicalName());
	
	private NettyClient nettyClient;
	private String gid;
	private String topic;
	
	private boolean isRunning = false;

	public DefaultProducer(){
		try {
//			LOG shutdown
			LogManager.getLogManager().reset();
//			String brokerIp = System.getProperty("SIP");
			String brokerIp = System.getProperty("SIP", "localhost");
			nettyClient = new NettyClient(brokerIp, BrokerEngine.PORT, this);
		} catch (Exception e) {
			LOG.severe(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void setTopic(String topic) {
		if(isRunning)
			throw new IllegalStateException("Producer already running");
		if(topic == null)
			throw new NullPointerException();
		this.topic = topic;
	}

	@Override
	public void setGroupId(String groupId) {
		if(isRunning)
			throw new IllegalStateException("Producer already running");
		if(groupId == null)
			throw new NullPointerException();
		this.gid = groupId;
	}

	@Override
	public SendResult sendMessage(Message message) {
		if(!isRunning)
			throw new IllegalStateException("Producer not started yet");
		message.setTopic(topic);

		SendResult res;
		try {
			res = nettyClient.sendMessage(message);
		} catch (Exception e) {
			LOG.severe(e.getMessage());
			res = new SendResult();
			res.setStatus(SendStatus.FAIL);
			res.setInfo(e.getMessage());
		}
		
		return res;
	}

	@Override
	public void asyncSendMessage(Message message, SendCallback callback) {
		if(!isRunning)
			throw new IllegalStateException("Producer not started yet");
		message.setTopic(topic);
		
		nettyClient.sendMessageAsync(message, callback);
	}

	@Override
	public void start() {
		if(isRunning)
			throw new IllegalStateException("Producer already running");
		if(gid == null || topic == null)
			throw new IllegalStateException("groupId or topic not set yet");
		
		try {
			nettyClient.start();
			nettyClient.sendMessage(ControlMessage.newRegisterMessage(gid, true));
		} catch (Exception e) {
			LOG.severe(e.getMessage());
			throw new RuntimeException(e);
		}
		
		isRunning = true;
	}

	@Override
	public void stop() {
		if(!isRunning)
			throw new IllegalStateException("Producer not running");
		isRunning = false;
		try {
			nettyClient.sendMessage(ControlMessage.newUnregisterMessage(true));
			nettyClient.stop();
		} catch (Exception e) {
			LOG.severe(e.getMessage());
			throw new RuntimeException(e);
		}
		LOG.info("Producer stopped");
	}
	
}
