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

import java.util.logging.LogManager;

import com.alibaba.middleware.race.mom.BrokerEngine;
import com.alibaba.middleware.race.mom.netty.kryo.KryoDecoder;
import com.alibaba.middleware.race.mom.netty.kryo.KryoEncoder;
import com.alibaba.middleware.race.mom.netty.kryo.KryoPool;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Echoes back any received data from a client.
 */
public final class NettyServer {
	private volatile boolean isRunning = false;
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private BrokerEngine brokerEngine = BrokerEngine.getInstance();
    
    public NettyServer(int port){
    	this.port = port;
    }
    
    public void start() throws InterruptedException{
    	if(isRunning) return;
    	synchronized (this) {
    		if(isRunning) return;
    		isRunning = true;
    		
    		// Configure the server.
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
//             .option(ChannelOption.SO_BACKLOG, 1024)
             .childOption(ChannelOption.SO_KEEPALIVE, true)
             .childOption(ChannelOption.TCP_NODELAY, true)
//             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                	 ch.closeFuture().addListener(new ChannelCloseListener(ch));
                	 
                	 ChannelPipeline p = ch.pipeline();
                	   KryoPool kryoPool = new KryoPool();
                     p.addLast(new KryoEncoder(kryoPool));
                     p.addLast(new KryoDecoder(kryoPool));
                     p.addLast(new NettyServerHandler());
                     
//                     /**
//                      * http-request解码器
//                      * http服务器端对request解码
//                      */
//                     p.addLast("decoder", new HttpRequestDecoder());
//                     
//                     p.addLast("servercodec",new HttpServerCodec()); 
//                     p.addLast("aggegator",new HttpObjectAggregator(1024*1024*64));//定义缓冲数据量 
//                     
//                     // httpRequestHandler
//                     p.addLast(new HttpRequestHandler());
//                     
//                     /**
//                      * http-response解码器
//                      * http服务器端对response编码
//                      */
//                     p.addLast("encoder", new HttpResponseEncoder());
                 }
             });

            // Start the server.
            ChannelFuture f = b.bind(port).sync();

            // Wait until the server socket is closed.
            // f.channel().closeFuture().sync();
		}
    }
    
    public void stop(){
    	if(! isRunning) return;
    	
    	synchronized (this) {
    		if(! isRunning) return;
    		isRunning = false;
    		
    		//TODO stop it. How to close netty?
    		// Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
		}
    }
    
    class ChannelCloseListener implements GenericFutureListener<Future<? super Void>>{
    	Channel channel;
    	ChannelCloseListener(Channel channel) {
    		this.channel = channel;
    	}
    	@Override
    	public void operationComplete(Future<? super Void> future) throws Exception {
//    		brokerEngine.channelUnregister(channel);
    	}
    }
}