package com.alibaba.middleware.race.mom.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.alibaba.middleware.race.mom.BrokerEngine;
/**
 * 采用三层管理方式
 * 1, QueueManager管理QueueGroup,每个QueueGroup的MessageQueue拥有一样的topic
 * 2, QueueGroup管理MessageQueue，MessageQuque由topic和queueId标识
 * @author wh
 *
 */
public class QueueManager {
	
	private List<QueueGroup> queueGroupList;
	private List<String> topicList ;
	private BrokerEngine brokerEngine;
	
	public QueueManager(BrokerEngine brokerEngine){
		this.brokerEngine = brokerEngine;
		this.queueGroupList  = new ArrayList<QueueGroup>();
		this.topicList = new ArrayList<String>();
	}
	
	/**
	 * 生成一个包含该topic的queueGroup
	 * @param topic
	 * @return
	 */
	public boolean register(String topic){
		if(! topicList.contains(topic)){
			QueueGroup queueGroup = new QueueGroup(topic);
			
			queueGroup.init();
			Map<String, Set<String>> TopicGroupIdMap = brokerEngine.getTopicGroupIdsMap();
			queueGroup.setTopicGroupIdMap(TopicGroupIdMap);
			
			topicList.add(topic);
			return queueGroupList.add(queueGroup);
		}
		
		//包含该topic的group已经存在，添加失败
		return false;
	}
	
	
	
	/**
	 * 根据topic获得QueueGroup
	 * @param topic
	 * @return
	 */
	public QueueGroup getQueueGroup(String topic){
		for(QueueGroup qg: queueGroupList){
			if(qg.getTopic().equals(topic)){
				return qg;		
			}	
		}
		return null;
	}
	/**
	 * 查找暂时使用顺序查找实现
	 * @param topic
	 * @param queueId
	 * @return
	 */
	public MessageQueue getMessageQueue(String topic, long queueId){
		
		for(QueueGroup qg: queueGroupList){
			if(qg.getTopic().equals(topic)){
				
				List<MessageQueue> messageQueueList = qg.getMessageQueueList();
				
				for(MessageQueue mq: messageQueueList){
					if(mq.getQueueId() == queueId){
						return mq;
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 获取保存该topic的queue group，随机返回group中一个队列
	 * @param topic
	 * @return
	 */
//	public MessageQueue getMessageQueue(String topic){
//		
//		for(QueueGroup qg: queueGroupList){
//			if(qg.getTopic() == topic){
//				MessageQueue messageQueue;
//				
//				do{
//					Random random = new Random();
//					int queueId = random.nextInt((int) qg.queueNumber());
//		        
//					messageQueue = qg.getMessageQueue(queueId);
//					
//				} while (messageQueue.isEmpty());
//				
//				return messageQueue;
//			}
//		}
//		
//		return null;
//	}
	
}
