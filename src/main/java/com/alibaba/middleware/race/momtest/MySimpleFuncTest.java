package com.alibaba.middleware.race.momtest;

import java.util.logging.Logger;

import com.alibaba.middleware.race.mom.ConsumeResult;
import com.alibaba.middleware.race.mom.Consumer;
import com.alibaba.middleware.race.mom.DefaultConsumer;
import com.alibaba.middleware.race.mom.DefaultProducer;
import com.alibaba.middleware.race.mom.Message;
import com.alibaba.middleware.race.mom.MessageListener;
import com.alibaba.middleware.race.mom.Producer;

public class MySimpleFuncTest {
	private static final Logger LOG = Logger.getLogger(MySimpleFuncTest.class.getCanonicalName());
	
	public static void main(String[] args) {
		Consumer consumer = new DefaultConsumer();
		Producer producer = new DefaultProducer();
		consumer.setGroupId("CID");
		producer.setGroupId("GID");
		consumer.subscribe("myTopic", null, new MessageListener() {
			@Override
			public ConsumeResult onMessage(Message message) {
				LOG.info("msg " + message.getMsgId() + " received");
				return null;
			}
		});
		consumer.start();
		
		producer.setTopic("myTopic");
		producer.start();
		Message msg = new Message();
		msg.setBody("hello world!".getBytes());
		producer.sendMessage(msg);
		LOG.info(msg.toString());
	}
}
