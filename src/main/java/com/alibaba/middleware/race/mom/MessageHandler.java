//package com.alibaba.middleware.race.mom;
//
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelHandlerContext;
//
//import java.util.Map;
//import java.util.logging.Logger;
//
//import com.alibaba.middleware.race.mom.netty.NettyServerHandler;
//import com.alibaba.middleware.race.mom.util.JsonParseUtil;
//import com.alibaba.middleware.race.mom.util.STATICVAL;
//
///**
// * MessageHandler
// * 
// * Handler json string from the httpRequestHandler convert json to message
// * 
// * @author keyofspectator
// *
// */
//public class MessageHandler {
//
//	private static final Logger LOG = Logger.getLogger(NettyServerHandler.class.getCanonicalName());
//	private final BrokerEngine brokerEngine = BrokerEngine.getInstance();
//	
//	public void messageHandler(String jsonStr , ChannelHandlerContext ctx) {
//		Channel channel = ctx.channel();
//		Object msg = jsonToMessage(jsonStr);
//		LOG.info("message read: \r\n" + msg);
//    	if(msg instanceof Message){
//	    	Message myMsg = (Message) msg;
////	    	
////	    	System.out.println("message id : " + myMsg.getMsgId() + " , message delay after netty server received : " + (System.currentTimeMillis() - myMsg.getBornTime()));
//	    	
//	    	brokerEngine.onMessageReceived(myMsg, ctx.channel());
//    	} else {
//    		ControlMessage myMsg = (ControlMessage) msg;
//    		brokerEngine.onCtrlMsgReceived(myMsg, channel);
//    	}
//		
//	}
//
//	public Object jsonToMessage(String jsonStr) {
//
//		Map entity = JsonParseUtil.toMap(jsonStr);
//		String gid = "";
//		
//		// groupID
//		if(entity.containsKey(STATICVAL.GROUPID)){
//			gid = (String) entity.get(STATICVAL.GROUPID);
//		}
//		
//		// message type
//		if(entity.get(STATICVAL.MESSAGETYPE) == STATICVAL.MESSAGETYPE_CTRLMESSAGE){ // CTRL Msg
//			ControlMessage ctrlMsg;
//			if(entity.get(STATICVAL.MSGCTRLTYPE) == STATICVAL.MSGCTRLTYPE_REG){
//				// Is productor or consumer
//				if(entity.get(STATICVAL.CLIENTTYPE) == STATICVAL.CLIENTTYPE_PRODUCTOR){
//					ctrlMsg = ControlMessage.newRegisterMessage(gid, true);
//				}
//				else if(entity.get(STATICVAL.CLIENTTYPE) == STATICVAL.CLIENTTYPE_CONSUMER){
//					ctrlMsg = ControlMessage.newRegisterMessage(gid, false);
//				}
//				else{
//					throw new Exception("MSG has no ClientType");  
//				}
//				return ctrlMsg;
//			}
//			else if(entity.get(STATICVAL.MSGCTRLTYPE)6h == STATICVAL.MSGCTRLTYPE_SUB){
//				if(entity.get(STATICVAL.CLIENTTYPE) == STATICVAL.CLIENTTYPE_CONSUMER){
//					ctrlMsg = ControlMessage.newRegisterMessage(gid, false);
//				}
//				else{
//					throw new Exception("MSG has no ClientType");  
//				}
//				return ctrlMsg;
//				
//			}
//			
//			// Is productor or consumer
//			if(entity.get(STATICVAL.CLIENTTYPE) == STATICVAL.CLIENTTYPE_PRODUCTOR){
//				ctrlMsg = ControlMessage.newRegisterMessage(gid, true);
//			}
//			else if(entity.get(STATICVAL.CLIENTTYPE) == STATICVAL.CLIENTTYPE_CONSUMER){
//				ctrlMsg = ControlMessage.newRegisterMessage(gid, false);
//			}
//			else{
//				throw new Exception("MSG has no ClientType");  
//			}
//			return ctrlMsg;
//		}
//		else{ // DATA Msg
//			
//			Message msg=new Message();
//			msg.setBody(BODY.getBytes(charset));
//			msg.setProperty("area", "hz"+code);
//			
//		}
//
//		Message msg = new Message();
//		msg.setBody(BODY.getBytes(charset));
//		msg.setProperty("area", "hz" + code);
//		
//		return msg;
//
//	}
//}
