package com.alibaba.middleware.race.mom.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.alibaba.middleware.race.mom.SendCallback;
import com.alibaba.middleware.race.mom.SendResult;

public class MessageFuture implements Future<SendResult>{
	private CountDownLatch latch;
	private SendCallback callback;
	private volatile SendResult result;
	
	public void finish(SendResult result){
		if(result == null)
			throw new NullPointerException();
		if(latch.getCount() == 0)
			return;
		
		this.result = result; 
		if(callback != null)
			callback.onResult(result);
		latch.countDown();
	}
	
//	public RpcFuture() {
//		latch = new CountDownLatch(1);
//		resp = null;
//	}
	
	public MessageFuture(SendCallback callback) {
		this.callback = callback;
		latch = new CountDownLatch(1);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return (latch.getCount() == 0);
	}

	@Override
	public SendResult get() throws InterruptedException, ExecutionException {
		latch.await();
		return result;
	}

	@Override
	public SendResult get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		if(timeout <= 0)
			latch.await();
		else
			latch.await(timeout, unit);
		return result;
	}

}
