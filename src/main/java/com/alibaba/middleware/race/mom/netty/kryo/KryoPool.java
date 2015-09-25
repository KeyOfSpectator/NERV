package com.alibaba.middleware.race.mom.netty.kryo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;

public class KryoPool {
	
	private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
	
	private KyroFactory kyroFactory;
	
	//TODO: how to config these?
	private int maxTotal = 100;
	
	private int minIdle = 20;
	
	private long maxWaitMillis = Long.MAX_VALUE;
	
	private long minEvictableIdleTimeMillis = 3000;
	
	public KryoPool() {
		kyroFactory = new KyroFactory(maxTotal, minIdle, maxWaitMillis, minEvictableIdleTimeMillis);
		
		Kryo[] kryos = new Kryo[20];
		
		for(int i=0; i<20; i++)
			kryos[i] = kyroFactory.getKryo();
		
		for(int i=0; i<20; i++)
			kyroFactory.returnKryo(kryos[i]);
		
		
	}
	
	public void encode(final ByteBuf out, final Object message) throws IOException {
		ByteBufOutputStream bout = new ByteBufOutputStream(out);
		bout.write(LENGTH_PLACEHOLDER);
		KryoSerialization kryoSerialization = new KryoSerialization(kyroFactory);
		kryoSerialization.serialize(bout, message);
	}
	
	public Object decode(final ByteBuf in) throws IOException {
		KryoSerialization kryoSerialization = new KryoSerialization(kyroFactory);
		return kryoSerialization.deserialize(new ByteBufInputStream(in));
	}
}
