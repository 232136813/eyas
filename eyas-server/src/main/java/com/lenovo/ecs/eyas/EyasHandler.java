package com.lenovo.ecs.eyas;

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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lenovo.ecs.eyas.exception.AvailabilityException;
import com.lenovo.ecs.eyas.exception.InvalidNameCharacterException;
import com.lenovo.ecs.eyas.exception.OperationNotSupportException;
import com.lenovo.ecs.eyas.exception.TooManyOpenReadsException;
import com.lenovo.ecs.eyas.thrift.Item;
import com.lenovo.ecs.eyas.thrift.QueueInfo;
import com.lenovo.ecs.eyas.thrift.Status;


public class EyasHandler {
	protected Logger log = LoggerFactory.getLogger(EyasHandler.class);	
	protected QueueCollection queues;
	protected Integer maxOpenReads;
	private Timer timer;
	protected String clientDesc;
	protected ServerStatus serverStatus;
	public EyasHandler(QueueCollection queueCollection, Timer timer, Integer maxOpenReads, String clientDesc, ServerStatus serverStatus){
		this.queues = queueCollection;
		this.timer = timer;
		this.maxOpenReads = maxOpenReads == null? Integer.MAX_VALUE : maxOpenReads;
		this.clientDesc = clientDesc;
	}
	

	
	final private void monitorUtil(String key, Date timeLimit, Integer maxItems, Boolean opening, List<Item> items) throws AvailabilityException {
		checkBlockReads("monitorUtil", key);
		if(log.isDebugEnabled())
			log.debug("monitor -> q={}, t={}, max ={}, open={}", new Object[]{key, timeLimit, maxItems, opening});
		
		for(int i=0; i< maxItems; i++){
			if(checkBlockReads("", key)){
				break;
			}
			if(log.isDebugEnabled())
				log.debug("monitor loop -> q={} t={} max={} open={}", new Object[]{key, timeLimit, maxItems, opening});
			if((maxItems == null || maxItems == 0) || (timeLimit != null && timeLimit.getTime() <= System.currentTimeMillis()) || 
					(countPendingReads(key) != null &&	countPendingReads(key) > maxOpenReads)){
				break;
			}else{
				Promise<QItem> promise = null;
				try {
					promise = queues.remove(key, timeLimit.getTime(), opening, false, null);
				} catch (InvalidNameCharacterException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				
				if(promise != null && promise.isDone()){
					QItem qItem = promise.get();
					if(qItem != null){
						Item item = new Item();
						item.setData(qItem.getData());
						item.setId(qItem.getXid());
						items.add(item);
					}

				}
			}
		
		}
	
	}
	
	public int put(String queue_name, List<ByteBuffer> items,
			int expiration_msec) {
	    int count = 0;
	    for(ByteBuffer item : items){
	    	byte[] data = new byte[item.remaining()];
	    	item.get(data);
	    	try {
	    		boolean result = queues.add(queue_name, data, expiration_msec == 0 ? null:(System.currentTimeMillis()+(long)expiration_msec), System.currentTimeMillis(), null);
	    		if(!result){
	    			return count;
	    		}
			} catch (Exception e) {	
				e.printStackTrace();
				return count;
			} 
	    	count++;
	    }
	    return count;
	}
	
	public List<Item> get(final String queue_name, int max_items, int timeout_msec,
			int auto_abort_msec) throws TException {
		final List<Item> items = new ArrayList<Item>();
		try {
			monitorUtil(queue_name, (timeout_msec == 0) ? null : new Date(System.currentTimeMillis()+(long)timeout_msec), max_items, auto_abort_msec > 0, items);
		} catch (Exception e) {
			e.printStackTrace();
			throw new TException("Service is not available");
		} 
		
		if(auto_abort_msec > 0){
			final Set<Integer> ids = new HashSet<Integer>();
			for(final Item item : items){
				ids.add(item.getId());
			}
			TimerTask task = new TimerTask(){
				@Override
				public void run() {
					try {
						abort(queue_name, ids);
					} catch (TException e) {
						e.printStackTrace();
						log.error("abortReads error queue_name = "+ queue_name + "; ids = "+ ids +"; items = " + items); 
					}
				}
			};
			timer.schedule(task, auto_abort_msec);
		}
		return items;
	}

	public int confirm(String queue_name, Set<Integer> xids) throws TException {
		int count = 0;

		if(xids != null){
			PersistentQueue queue = null;
			try {
				queue = queues.queue(queue_name, false);
			} catch (Exception e) {
				e.printStackTrace();
				throw new TException(e);
			} 			
			for(Integer xid : xids)
			try {	
				if(xid != null && queue != null){
					queue.confirmRemove(xid);
					count++;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new TException(e);
			} 
		}
		return count;
	}


	public int abort(String queue_name, Set<Integer> xids) throws TException {
		int count = 0;
		if(xids != null){
			PersistentQueue queue = null;
			try {
				queue = queues.queue(queue_name, false);
			} catch (Exception e) {
				e.printStackTrace();
				throw new TException(e);
			} 			
			for(Integer xid : xids)
			try {	
				if(xid != null && queue != null){
					queue.unremove(xid);
					count++;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new TException(e);
			} 
		}
		return count;
	}
	
	private Integer countPendingReads(String key){
		try {
			PersistentQueue queue = queues.queue(key, false);
			if(queue != null){
				return queue.openTransactionCount();
			}
		} catch (InvalidNameCharacterException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return  null;
	}
	
	
	protected boolean checkBlockReads(String op, String key) throws AvailabilityException{
		if(serverStatus != null && serverStatus.isBlockReads()){
			if(log.isDebugEnabled())
				log.debug("Blocking {} on '{}' (sid {}, {})", new Object[]{op, key, null});
			throw new AvailabilityException(op);
		}
		return false;
	}
	
	protected void checkBlockWrites(String op, String key) throws AvailabilityException{
	    if (serverStatus != null && serverStatus.isBlockWrites()) {
			if(log.isDebugEnabled())
				log.debug("Blocking {} on '{}' (sid {}, {})", new Object[]{op, key, null});
	        throw new AvailabilityException(op);
	    }
	}

	
	private void abort(String queue_name, Integer xid) throws InvalidNameCharacterException, IOException{
		if(xid == null)return;
		PersistentQueue queue = queues.queue(queue_name, false);
		if(queue != null)
			queue.unremove(xid);
	}	
	
	public QueueInfo peek(String queue_name) {
		throw new OperationNotSupportException();
	}

	public void flush_queue(String queue_name)  {
		throw new OperationNotSupportException();
	}

	public void flush_all_queues()  {
		throw new OperationNotSupportException();
	}

	public void delete_queue(String queue_name) {
		throw new OperationNotSupportException();
	}

	public Status current_status() {
		throw new OperationNotSupportException();
	}

	public void set_status(Status status) {
		throw new OperationNotSupportException();
	}

	public String get_version() {
		throw new OperationNotSupportException();
	}
}
