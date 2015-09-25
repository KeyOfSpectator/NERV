/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.alibaba.middleware.race.mom.netty;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.alibaba.middleware.race.mom.AbstractMessage;
import com.alibaba.middleware.race.mom.BrokerEngine;
import com.alibaba.middleware.race.mom.Consumer;
import com.alibaba.middleware.race.mom.ControlMessage;
import com.alibaba.middleware.race.mom.Message;
import com.alibaba.middleware.race.mom.Producer;
import com.alibaba.middleware.race.mom.SendCallback;
import com.alibaba.middleware.race.mom.SendResult;
import com.alibaba.middleware.race.mom.SendStatus;
import com.alibaba.middleware.race.mom.async.MessageFuture;
import com.alibaba.middleware.race.mom.netty.kryo.KryoDecoder;
import com.alibaba.middleware.race.mom.netty.kryo.KryoEncoder;
import com.alibaba.middleware.race.mom.netty.kryo.KryoPool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Sends one message when a connection is open and echoes back any received
 * data to the server.  Simply put, the echo client initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public final class NettyClient {
	private static final Logger LOG = Logger.getLogger(NettyClient.class.getCanonicalName());
	
	private final String host;
	private final int port;
	private Channel channel;
	private boolean isRunning = false;
	private EventLoopGroup group;
	private Map<String, MessageFuture> futureMap;
	private boolean isProducer;
	
	private Producer producer;
	private Consumer consumer;
    
    private NettyClient(String host, int port, boolean isProducer) throws Exception {
		this.host = host;
		this.port = port;
		this.isProducer = isProducer;
		futureMap = new ConcurrentHashMap<String, MessageFuture>();
	}
    
    public NettyClient(String host, int port, Producer producer) throws Exception{
    	this(host, port, true);
    	this.producer = producer;
    }
    
    public NettyClient(String host, int port, Consumer consumer) throws Exception{
    	this(host, port, false);
    	this.consumer = consumer;
    }
    
    private void dealWithAckMsg(ControlMessage ctrlMsg){
    	String msgId = ctrlMsg.getMsgId();
		MessageFuture future = futureMap.remove(msgId);
		SendResult result = new SendResult();
		result.setMsgId(msgId);
		if(ctrlMsg.isSucceeded())
			result.setStatus(SendStatus.SUCCESS);
		else{
			result.setStatus(SendStatus.FAIL);
			result.setInfo(ctrlMsg.getErrorMsg().getMessage());
		}
		future.finish(result);
    }
    
    private void dealWithMessage(Message msg){
    	consumer.onMessageReceived(msg);
    }
    
    public void dealWithResponse(Object resp){
    	if(isProducer){
    		dealWithAckMsg((ControlMessage)resp);
    	} else {
    		if(resp instanceof Message)
    			dealWithMessage((Message)resp);
    		else
    			dealWithAckMsg((ControlMessage)resp);
    	}
    }
    
    public MessageFuture sendMessageAsync(AbstractMessage msg, SendCallback callback){
    	MessageFuture future = new MessageFuture(callback);
    	futureMap.put(msg.getMsgId(), future);
    	LOG.info("client writing msg: " + msg);
    	channel.writeAndFlush(msg);
    	return future;
    }
    
    public SendResult sendMessage(AbstractMessage msg) throws InterruptedException, ExecutionException{
    	return sendMessageAsync(msg, null).get();
    }

	public void start() throws Exception {
		if(isRunning)
			throw new IllegalStateException();
        // Configure the client.
        group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.TCP_NODELAY, true)
         .option(ChannelOption.SO_KEEPALIVE, true)
         .handler(new ChannelInitializer<SocketChannel>() {
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
            	 KryoPool kryoPool = new KryoPool();
                 ChannelPipeline p = ch.pipeline();
                 p.addLast(new KryoEncoder(kryoPool));
                 p.addLast(new KryoDecoder(kryoPool));
                 p.addLast(new NettyClientHandler(NettyClient.this));
             }
         });

        // Start the client.
        ChannelFuture f = b.connect(host, port).sync();
        channel = f.channel();
        
        isRunning = true;
    }
	
	public void stop() throws InterruptedException{
		if(!isRunning)
			throw new IllegalStateException();
		channel.close().sync();
		group.shutdownGracefully();
		
		channel = null;
		group = null;
		futureMap.clear();
		producer = null;
		consumer = null;
		isRunning = false;
	}
}
