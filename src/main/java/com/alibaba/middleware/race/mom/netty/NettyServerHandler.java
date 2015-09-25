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

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import com.alibaba.middleware.race.mom.BrokerEngine;
import com.alibaba.middleware.race.mom.ControlMessage;
import com.alibaba.middleware.race.mom.Message;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handler implementation for the echo server.
 */
@Sharable
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
	private static final Logger LOG = Logger.getLogger(NettyServerHandler.class.getCanonicalName());
	private final BrokerEngine brokerEngine = BrokerEngine.getInstance();
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    		throws UnsupportedEncodingException {
    	Channel channel = ctx.channel();
    	LOG.info("message read: \r\n" + msg);
    	if(msg instanceof Message){
	    	Message myMsg = (Message) msg;
//	    	
//	    	System.out.println("message id : " + myMsg.getMsgId() + " , message delay after netty server received : " + (System.currentTimeMillis() - myMsg.getBornTime()));
	    	
	    	brokerEngine.onMessageReceived(myMsg, ctx.channel());
    	} else {
    		ControlMessage myMsg = (ControlMessage) msg;
    		brokerEngine.onCtrlMsgReceived(myMsg, channel);
    	}
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        
        brokerEngine.onChannelCloseAbnormally(ctx.channel());
        ctx.close();
    }
}
