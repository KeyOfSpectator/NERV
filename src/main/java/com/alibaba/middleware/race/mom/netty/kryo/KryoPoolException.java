package com.alibaba.middleware.race.mom.netty.kryo;

public final class KryoPoolException extends RuntimeException {
	
	private static final long serialVersionUID = -2992257109597526961L;
	
	public KryoPoolException(final Exception cause) {
		super(cause);
	}
}
