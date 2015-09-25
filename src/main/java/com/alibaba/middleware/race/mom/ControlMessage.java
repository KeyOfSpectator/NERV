package com.alibaba.middleware.race.mom;

public class ControlMessage extends AbstractMessage{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4123118100617377845L;
	public static enum CtrlType{REG, UNREG, SUB, UNSUB, ACK , ACK_MSG};
	
	private CtrlType ctrlType;
	private String groupId;
	private String topic;
	private String filterKey;
	private String filterValue;
	private Boolean isProducer;
	private long originalMsgBornTime;
	

	private Boolean succeeded;
	private CtrlMsgException errorMsg;
	
	public boolean isProducer(){
		return isProducer;
	}
	public boolean isSucceeded() {
		return succeeded;
	}
	
	public long getOriginalMsgBornTime() {
		return originalMsgBornTime;
	}
	
	public CtrlMsgException getErrorMsg() {
		return errorMsg;
	}
	
	public CtrlType getCtrlType() {
		return ctrlType;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getTopic() {
		return topic;
	}

	public String getFilterKey() {
		return filterKey;
	}

	public String getFilterValue() {
		return filterValue;
	}
	
	public static ControlMessage newRegisterMessage(String groupId, boolean isProducer){
		if(groupId == null)
			throw new NullPointerException();
		ControlMessage msg = new ControlMessage();
		msg.msgId = genNewMsgId();
		msg.ctrlType = CtrlType.REG;
		msg.groupId = groupId;
		msg.isProducer = isProducer;
		
		return msg;
	}
	
	public static ControlMessage newUnregisterMessage(boolean isProducer){
		ControlMessage msg = new ControlMessage();
		msg.msgId = genNewMsgId();
		msg.ctrlType = CtrlType.UNREG;
		msg.isProducer = isProducer;
		
		return msg;
	}
	
	public static ControlMessage newSubscribeMessage(String topic){
		return newSubscribeMessage(topic, null, null);
	}
	
	public static ControlMessage newSubscribeMessage(
			String topic, String filterKey, String filterValue){
		if(topic == null)
			throw new NullPointerException();
		if((filterKey == null) ^ (filterValue == null))
			throw new IllegalArgumentException();
		
		ControlMessage msg = new ControlMessage();
		msg.msgId = genNewMsgId();
		msg.ctrlType = CtrlType.SUB;
		msg.topic = topic;
		msg.filterKey = filterKey;
		msg.filterValue = filterValue;
		
		return msg;
	}
	
	public static ControlMessage newUnsubscribeMessage(String topic){
		if(topic == null)
			throw new NullPointerException();
		
		ControlMessage msg = new ControlMessage();
		msg.msgId = genNewMsgId();
		msg.ctrlType = CtrlType.UNSUB;
		msg.topic = topic;
		
		return msg;
	}
	
	public static ControlMessage newAckMessage(String msgId){
		if(msgId == null)
			throw new NullPointerException();
		
		ControlMessage msg = new ControlMessage();
		msg.ctrlType = CtrlType.ACK;
		msg.msgId = msgId;
		msg.succeeded = true;
		
		return msg;
	}
	
	public static ControlMessage newAckMSGMessage(String msgId , long bornTime){
		if(msgId == null)
			throw new NullPointerException();
		
		ControlMessage msg = new ControlMessage();
		msg.ctrlType = CtrlType.ACK_MSG;
		msg.msgId = msgId;
		msg.succeeded = true;
		msg.originalMsgBornTime = bornTime;
		
		return msg;
	}
	
	public static ControlMessage newFailMessage(CtrlMsgException error, String msgId){
		if(msgId == null || error == null)
			throw new NullPointerException();
		
		ControlMessage msg = new ControlMessage();
		msg.ctrlType = CtrlType.ACK;
		msg.errorMsg = error;
		msg.msgId = msgId;
		msg.succeeded = false;
		
		return msg;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(ControlMessage.class.getSimpleName()).append(": \r\n[");
		sb.append("msgId: ").append(msgId).append(", ");
		sb.append("ctrlType: ").append(ctrlType).append(", ");
		sb.append("groupId: ").append(groupId).append(", ");
		sb.append("topic: ").append(topic).append(", ");
		sb.append("filterKey: ").append(filterKey).append(", ");
		sb.append("filterValue: ").append(filterValue).append(", ");
		sb.append("isProducer: ").append(isProducer).append(", ");
		sb.append("succeeded: ").append(succeeded).append(", ");
		sb.append("errorMsg: ").append(errorMsg).append("]");
		
		return sb.toString();
	}
	
	private static String genNewMsgId(){
		return ""+ATOMIC_LONG.incrementAndGet();
	}
}
