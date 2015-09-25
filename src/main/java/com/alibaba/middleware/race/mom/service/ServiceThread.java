package com.alibaba.middleware.race.mom.service;


/**
 *  后台服务线程基类
 * @author fsc
 *
 */
public class ServiceThread implements Runnable{
	
	// 执行线程
    protected final Thread thread;
    
    /**
     * 构造函数 创建线程
     */
    public ServiceThread() {
        this.thread = new Thread(this, this.getServiceName());
    }
    
	private String getServiceName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
