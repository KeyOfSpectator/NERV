package com.alibaba.middleware.race.mom.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.middleware.race.mom.Message;

public class QueueGroup {
	private List<MessageQueue> MessageQueueList = new ArrayList<MessageQueue>();
	final private String topic;
	private int defaultQueueNumber;
	
	//维护每个group对message的消费进度, (groupId, (msgId, true/false))
	private HashMap<String, HashMap<String, Boolean>> consumeProgress;
	private Map<String, Set<String>> TopicGroupIdMap;
	
	public QueueGroup(String topic){
		this.topic = topic;
		defaultQueueNumber = 3;
	}
	
	public Map<String, Set<String>> setTopicGroupIdMap(Map<String, Set<String>> TopicGroupIdMap){
		return this.TopicGroupIdMap = TopicGroupIdMap;
	}
	/**
	 * QueueGroup初始化， 生成默认数量的MessageQueue
	 */
	public void init(){
		for(int queueId=0; queueId<defaultQueueNumber; queueId++){
			MessageQueue messageQueue = new MessageQueue(queueId, topic);
			consumeProgress = new HashMap<String, HashMap<String, Boolean>>();
			MessageQueueList.add(messageQueue);
		}
			
	}
	
	/**
	 * 获得这个group订阅的topic中没有消费过的MsgId
	 * @param groupId
	 * @return
	 */
	public String getAvalibleMessageId(String groupId){
		
		HashMap<String, Boolean> msgIdMap = consumeProgress.get(groupId);
		Boolean False = Boolean.valueOf(false);

		for (Map.Entry<String, Boolean> msgId : msgIdMap.entrySet()) {  
				  
//			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
			if(msgId.getValue().equals(False)){
				return msgId.getKey();
			}
		}
		
		return null;		
	}
	
	/**
	 * 通过MsgId在队列中获得MessageIndex
	 * @param msgId
	 * @return
	 */
	public MessageIndex getMessageIndex(String msgId){
		
		return this.getMessageIndexByMsgId(msgId);
	}
	
	/**
	 * 根据Broker传给我的message来维护consumeProgress
	 * @param message
	 */
	public void adapt(Message message){
		String msgId = message.getMsgId();
		String topic = message.getTopic();
		Set<String> groupIdSet = TopicGroupIdMap.get(topic);
		
		Boolean False = Boolean.valueOf(false);
		for(String groupId : groupIdSet){
			HashMap<String, Boolean> msgIdMaps = consumeProgress.get(groupId);
			
			if(msgIdMaps==null){
				msgIdMaps = new HashMap<String, Boolean>();
			}
			
			msgIdMaps.put(msgId, False);
			consumeProgress.put(groupId, msgIdMaps);
		}
	}
	
	/**
	 * 设置groupId对message的消费情况
	 * @param message
	 * @param groupId
	 * @param consumeResult
	 */
	public void setMsgIdMap(String msgId, String groupId, boolean consumeResult){
		Boolean ConsumeResult = Boolean.valueOf(consumeResult);
//		String msgId = message.getMsgId();
		
		HashMap<String, Boolean> MsgIdMap = consumeProgress.get(groupId);
		MsgIdMap.put(msgId, ConsumeResult);
		
	}
	
	/**
	 * 通过index获得messageQueue
	 * @param index
	 * @return
	 */
	public MessageQueue getMessageQueue(int index){
		return MessageQueueList.get(index);
	}
	
	public String getTopic(){
		return topic;
	}
	
	public List<MessageQueue> getMessageQueueList(){
		return MessageQueueList;
	}
	
	
//	public boolean register(MessageQueue messageQueue){
//		return MessageQueueList.add(messageQueue);
//	}
	
	public long queueNumber(){
		return MessageQueueList.size();
	}
	
	/**
	 * 通过msgId获得队列中的messageIndex
	 * @param msgId
	 * @return
	 */
	public MessageIndex getMessageIndexByMsgId(String msgId){
		
		for(MessageQueue messageQueue : MessageQueueList){
			List<MessageIndex> messageIndexQueue = messageQueue.getMessageIndexQueue();
			
			for(MessageIndex messageIndex : messageIndexQueue){
				if(messageIndex.getMsgId() == msgId){
					
					return messageIndex;
				}
			}
		}
		
		return null;
	}
}
