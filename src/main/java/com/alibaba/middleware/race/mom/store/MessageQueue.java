package com.alibaba.middleware.race.mom.store;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.alibaba.middleware.race.mom.Message;

/**
 * 1, MessageQueue负责实现一个消息队列
 * 2, Queue里存放每个消息的offset和length，即存放的是消息的索引（index）,用于指定消息在物理文件的位置
 * 3, Queue暂时使用BlockingQueue实现，保存MessageIndex
 * @author wh
 *
 */
public class MessageQueue {
	final private long queueId;
	final private String topic;
	final private int MAX_BLOCKINGQUEUE_SIZE = 1024;
	
	final List<MessageIndex> messageIndexQueue = new ArrayList<MessageIndex>();
	
	MessageQueue(long queueId, String topic){
		this.queueId = queueId;
		this.topic = topic;
	}
	
	public long getQueueId(){
		return queueId;
	}
	
	/**
	 * 将Message生成的MessageIndex推进队列，并将消息持久化
	 * @param message
	 */
	public void add(MessageIndex messageIndex){
		messageIndexQueue.add(messageIndex);
	}
	
	public MessageIndex get(int index){
		return messageIndexQueue.get(index);
	}
	
	public List<MessageIndex> getMessageIndexQueue(){
		return messageIndexQueue;
	}
	
	public int length(){
		return messageIndexQueue.size();
	}
	
	public void print(){
		for(MessageIndex messageIndex : messageIndexQueue){
			System.out.println(messageIndex.getMsgId() + " " + messageIndex.getOffset() + " " + messageIndex.getLength());
		}
	}
	
	public boolean isEmpty(){
		return messageIndexQueue.size() == 0 ? true : false;
	}
	
	
}
