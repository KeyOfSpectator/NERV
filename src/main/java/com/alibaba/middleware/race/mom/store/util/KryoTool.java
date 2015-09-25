package com.alibaba.middleware.race.mom.store.util;

import java.util.HashMap;

import com.alibaba.middleware.race.mom.Message;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

public class KryoTool {
	private static final KryoTool KRYO_TOOL = new KryoTool();
	private static final int INIT_KRYO_NUM = 10;
	public static KryoTool getInstance(){
		return KRYO_TOOL;
	}
	
	private KryoPool pool;
	
	private KryoTool(){
		KryoFactory factory = new KryoFactory() {
			public Kryo create () {
				return new Kryo();
			}
		};
		pool = new KryoPool.Builder(factory).softReferences().build();
		Kryo[] initKryos = new Kryo[INIT_KRYO_NUM];
		for(int i=0; i<INIT_KRYO_NUM; i++)
			initKryos[i] = pool.borrow();
		for(int i=0; i<INIT_KRYO_NUM; i++)
			pool.release(initKryos[i]);
	}
	
	public static void main(String[] args) {  
		KryoTool kryoTool = new KryoTool();
		
		Message msg = new Message();
		msg.setTopic("test");
		
		byte[] b = kryoTool.encode(msg);
		String topic = kryoTool.decode(b).getTopic();
		
		System.out.println(topic);
	}
	
	//序列化
	public byte[] encode(HashMap<String, HashMap<String, Boolean>> consumeProgress){
		Kryo kryo = new Kryo();
		Registration registration = kryo.register(HashMap.class);
				
		Output output = null;
		output = new Output(1, 4096);
		kryo.writeObject(output, consumeProgress);
			
		byte[] b = output.toBytes();
			
		output.flush();
		return b;
			
	}
	
	//序列化
	public byte[] encode(Message msg){
		Kryo kryo = pool.borrow();
		try{
			Output output = null;
			output = new Output(1, 4096);
			kryo.writeObject(output, msg);
			
			byte[] b = output.toBytes();
			
			output.flush();
			return b;
		} finally {
			pool.release(kryo);
		}
	}
	
	//反序列化
	public Message decode(byte[] b){
		Kryo kryo = pool.borrow();
		try{
//			Registration registration = kryo.register(Message.class);
			
			Input input = null;
			
			input = new Input(b);
			Message msg = (Message) kryo.readObject(input, Message.class);
			input.close();
			
			return msg;
		} finally {
			pool.release(kryo);
		}
	}
	
//	public void serialize(final ByteBuf out, final Object msg){
//		Kryo kryo = pool.borrow();
//		try{
//			ByteBufOutputStream bout = new ByteBufOutputStream(out);
//			Output output = new Output(bout);
//			kryo.writeClassAndObject(output, msg);
//			output.close();
//		} finally {
//			pool.release(kryo);
//		}
//	}
//	
//	public Object deserialize(final ByteBuf in) throws IOException {
//		Kryo kryo = pool.borrow();
//		try{
//			Input input = new Input(new ByteBufInputStream(in));
//			Object result = kryo.readClassAndObject(input);
//			input.close();
//			
//			return result;
//		} finally {
//			pool.release(kryo);
//		}
//	}
}
