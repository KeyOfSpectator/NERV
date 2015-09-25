package com.alibaba.middleware.race.mom;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ReSendControler {

	private final ScheduledExecutorService scheduler;
	private final ReSendHandler reSendHandler;
	
	// config scheduleAtFixedRate
	private int ReSendHandlerInitialDelay;
	private int ReSendHandlerPeriodDelay;
	private TimeUnit ReSendHandlerUnit;

	public ReSendControler(int TreadNum , ReSendHandler reSendHandler) {
		// TODO Auto-generated constructor stub
		scheduler =Executors.newScheduledThreadPool(TreadNum);
		this.reSendHandler = reSendHandler;
		
		// config
		ReSendHandlerInitialDelay = 10;
		ReSendHandlerPeriodDelay = 0;
		ReSendHandlerUnit = TimeUnit.MILLISECONDS;
	}

	public void startReSend(){
//		     final ScheduledFuture<?> ReSendHandle = 
		    		 scheduler.scheduleWithFixedDelay(reSendHandler, ReSendHandlerInitialDelay, ReSendHandlerPeriodDelay, ReSendHandlerUnit);
//		     scheduler.schedule(new Runnable() {
//		       public void run() { ReSendHandle.cancel(true); }
//		     }, 60 * 60, TimeUnit.MILLISECONDS);
	}
	
}


/** ReSend Event
 * 
 * @author fsc
 *
 */
class ReSendHandler implements Runnable{

	private int delay;
	
	public ReSendHandler() {
		// TODO Auto-generated constructor stub
	}

	public void setSleepDelay(int sleepDelay){
		this.delay = sleepDelay;
	}
	
	@Override
	public void run() {
		// TODO ReSend periodly
		if(!BrokerEngine.getInstance().isAccumulateEmpty()){
			try {
				BrokerEngine.getInstance().onSendAccumulateMsg();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
