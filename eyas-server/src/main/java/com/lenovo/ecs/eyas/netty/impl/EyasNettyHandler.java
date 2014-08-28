package com.lenovo.ecs.eyas.netty.impl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import com.lenovo.ecs.eyas.EyasHandler;
import com.lenovo.ecs.eyas.IEyasHandler;
import com.lenovo.ecs.eyas.QueueCollection;
import com.lenovo.ecs.eyas.ServerStatus;
import com.lenovo.ecs.eyas.thrift.Item;

import io.netty.handler.codec.memcache.binary.BinaryMemcacheServerCodec;

public class EyasNettyHandler extends BinaryMemcacheServerCodec implements IEyasHandler{

	private EyasHandler handler;

	public EyasNettyHandler(QueueCollection queueCollection, Timer timer, Integer maxOpenReads,  ServerStatus serverStatus){
		this.handler = new EyasHandler(queueCollection, timer, maxOpenReads, null, serverStatus);
	}


	
	
	
	@Override
	public int put(String queue_name, List<ByteBuffer> items,
			int expiration_msec) {
		return 0;
	}





	@Override
	public List<Item> get(String queue_name, int max_items, int timeout_msec,
			int auto_abort_msec) {
		return null;
	}





	@Override
	public int confirm(String queue_name, Set<Integer> xids) {
		return 0;
	}





	@Override
	public int abort(String queue_name, Set<Integer> xids) {
		return 0;
	}


}
