package com.alibaba.middleware.race.mom;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class AbstractMessage implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6606138900733396585L;
	
	protected static final AtomicLong ATOMIC_LONG = new AtomicLong();
	//全局唯一的消息id，不同消息不能重复
	protected String msgId;

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
}
