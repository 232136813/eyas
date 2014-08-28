package com.lenovo.ecs.eyas.thrift.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lenovo.ecs.eyas.EyasHandler;
import com.lenovo.ecs.eyas.EyasServer;
import com.lenovo.ecs.eyas.PersistentQueue;
import com.lenovo.ecs.eyas.Promise;
import com.lenovo.ecs.eyas.QItem;
import com.lenovo.ecs.eyas.QueueCollection;
import com.lenovo.ecs.eyas.ServerStatus;
import com.lenovo.ecs.eyas.exception.AvailabilityException;
import com.lenovo.ecs.eyas.exception.InvalidNameCharacterException;
import com.lenovo.ecs.eyas.exception.OperationNotSupportException;
import com.lenovo.ecs.eyas.thrift.Item;
import com.lenovo.ecs.eyas.thrift.QueueInfo;
import com.lenovo.ecs.eyas.thrift.Status;
import com.lenovo.ecs.eyas.thrift.Eyas;

public class EyasThriftHandler extends EyasHandler implements Eyas.Iface{

	private EyasHandler handler;
	private static final Logger log = LoggerFactory.getLogger(EyasThriftHandler.class);
	
	public EyasThriftHandler(QueueCollection queueCollection, Timer timer, Integer maxOpenReads,  ServerStatus serverStatus){
		this.serverStatus = serverStatus;
		this.handler = new EyasHandler(queueCollection, timer, maxOpenReads, null, serverStatus);
	}

	@Override
	public int put(String queue_name, List<ByteBuffer> items,
			int expiration_msec) throws TException {
		return 0;
	}

	@Override
	public List<Item> get(String queue_name, int max_items, int timeout_msec,
			int auto_abort_msec) throws TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int confirm(String queue_name, Set<Integer> ids) throws TException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int abort(String queue_name, Set<Integer> ids) throws TException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public QueueInfo peek(String queue_name) throws TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void flush_queue(String queue_name) throws TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush_all_queues() throws TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete_queue(String queue_name) throws TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Status current_status() throws TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set_status(Status status) throws TException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String get_version() throws TException {
		return null;
	}


	
}
