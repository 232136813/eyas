package com.lenovo.ecs.eyas;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import com.lenovo.ecs.eyas.thrift.Item;

public interface IEyasHandler {
	public int put(String queue_name, List<ByteBuffer> items,int expiration_msec) ;
	public List<Item> get(final String queue_name, int max_items, int timeout_msec, int auto_abort_msec);
	public int confirm(String queue_name, Set<Integer> xids) ;
	public int abort(String queue_name, Set<Integer> xids) ;
}
