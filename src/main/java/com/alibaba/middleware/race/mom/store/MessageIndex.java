package com.alibaba.middleware.race.mom.store;

/**
 * MessageIndex记录Message在文件系统中的偏移位置(offset)和长度(length)以便可以快速从文件系统中读取该Message
 * @author wh
 *
 */
public class MessageIndex {
	
	private String msgId;
	private long offset;
	private long length;
	
	public MessageIndex(String msgId, long offset, long length){
		this.msgId = msgId;
		this.offset = offset;
		this.length = length;
	}
	
	public String getMsgId(){
		return msgId;
	}
	
	public long getOffset(){
		return offset;
	}
	
	public long getLength(){
		return length;
	}
}
