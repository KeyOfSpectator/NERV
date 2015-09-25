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
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
	
	private static final Logger LOG = Logger.getLogger(NettyClientHandler.class.getCanonicalName());
	private final NettyClient client;

    public NettyClientHandler(NettyClient client) {
		this.client = client;
	}
    
//    @Override
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//    	InvocationReq req = new InvocationReq();
//    	req.setContext("test key", "test val");
//    	ctx.writeAndFlush(req);
//    	LOG.info("test addProp sent");
//    };
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException {
    	LOG.info("message read: \r\n" + msg);
    	client.dealWithResponse(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
       ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
