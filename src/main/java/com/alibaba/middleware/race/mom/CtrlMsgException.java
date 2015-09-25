package com.alibaba.middleware.race.mom;

public class CtrlMsgException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1456418876429380908L;

	public CtrlMsgException() {
        super();
    }
	
	public CtrlMsgException(String message) {
        super(message);
    }
	
	public CtrlMsgException(String message, Throwable cause) {
        super(message, cause);
    }
	
	public CtrlMsgException(Throwable cause) {
        super(cause);
    }
	
}
