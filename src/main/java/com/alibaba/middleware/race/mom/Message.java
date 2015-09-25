package com.alibaba.middleware.race.mom;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Message extends AbstractMessage{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5295808332504208830L;
	
	private String topic;
	private byte[] body;
	private long bornTime;
	
	private Map<String, String> properties = new HashMap<String, String>();
	
	//维护每个group对message的消费进度, (groupId, true/false)
//	private Map<String, Boolean> consumeProgress = new HashMap<String, Boolean>();
	
	public Message(){
		setMsgId(""+ATOMIC_LONG.incrementAndGet());
		setBornTime(System.currentTimeMillis());
	}
	
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public String getTopic() {
		return topic;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public byte[] getBody() {
		return body;
	}

	public String getProperty(String key) {
		return properties.get(key);
	}
	/**
	 * 设置消息属性
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value) {
		properties.put(key, value);
	}
	/**
	 * 删除消息属性
	 * @param key
	 */
	public void removeProperty(String key) {
		properties.remove(key);
	}
	public long getBornTime() {
		return bornTime;
	}
	public void setBornTime(long bornTime) {
		this.bornTime = bornTime;
	}
	
//	public String getAviableConsumerGid(){
//		
//		Boolean False = Boolean.valueOf(false);
//		
//		for (Entry<String, Boolean> entry : consumeProgress.entrySet()) {  
//			  
////		    System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 
//			if(entry.getValue().equals(False)){
//				//表明当前gid没有接受过该消息
//				return entry.getKey();
//			}
//		}//for
//		
//		//当前消息被所有groupId都消费过
//		return null;
//	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(Message.class.getSimpleName()).append(": \r\n[");
		sb.append("msgId: ").append(msgId).append(", ");
		sb.append("topic: ").append(topic).append(", ");
		sb.append("body: ").append(new String(body)).append(", ");
		sb.append("properties: ").append(properties).append("]");
		
		return sb.toString();
	}
}
