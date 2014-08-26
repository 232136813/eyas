package com.lenovo.ecs.eyas;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lenovo.ecs.eyas.exception.AvailabilityException;
import com.lenovo.ecs.eyas.exception.InvalidNameCharacterException;
import com.lenovo.ecs.eyas.exception.TooManyOpenReadsException;

@Deprecated

public class EyasHandler {
	protected Logger log = LoggerFactory.getLogger(EyasHandler.class);
//	private AtomicBoolean finished = new AtomicBoolean(false);
//	volatile Promise<QItem> waitingFor = null;
	
	protected QueueCollection queues;
	protected Integer maxOpenReads;
	protected String clientDesc;
	protected ServerStatus serverStatus;
	public EyasHandler(QueueCollection queueCollection, Integer maxOpenReads, String clientDesc, ServerStatus serverStatus){
		this.queues = queueCollection;
		this.maxOpenReads = maxOpenReads;
		this.clientDesc = clientDesc;
	}
	

	
	public void flushAllQueues() throws AvailabilityException{
		checkBlockWrites("flushAll", "<all>");
		queues.queueNames();
	}
	
	
	protected Integer countPendingReads(String key){
//		queues.
		return  null;
	}

	
	
	final public void monitorUtil(String key, Date timeLimit, Integer maxItems, Boolean opening, MethodPack pack) throws AvailabilityException {
		checkBlockReads("monitorUtil", key);
		if(log.isDebugEnabled())
			log.debug("monitor -> q={}, t={}, max ={}, open={}", new Object[]{key, timeLimit, maxItems, opening});
		
		for(int i=0; i< maxItems; i++){
			if(safeCheckBlockReads()){
				pack.methodInEyasHandler(null, null);
				break;
			}
			if(log.isDebugEnabled())
				log.debug("monitor loop -> q={} t={} max={} open={}", new Object[]{key, timeLimit, maxItems, opening});
			if((maxItems == null || maxItems == 0) || (timeLimit != null && timeLimit.getTime() <= System.currentTimeMillis()) || 
					(countPendingReads(key) != null &&	countPendingReads(key) > maxOpenReads)){
				pack.methodInEyasHandler(null, null);
				break;
			}else{
				Promise<QItem> promise = null;
				try {
					promise = queues.remove(key, timeLimit.getTime(), opening, false, clientDesc);
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
					QItem item = promise.get();
					if(item == null){
						pack.methodInEyasHandler(null, null);
					}else{
						Long xidContext = null;
//						if(opening)xidContext = addPendingRead(key, item.getXid());
						pack.methodInEyasHandler(item, item.getXid());
						
					}
				}
			}
		
		}
	
	}
	public Promise<QItem> getItem(String key, Date timeout, Boolean opening, Boolean peeking) throws TooManyOpenReadsException, AvailabilityException, InvalidNameCharacterException, IOException, InterruptedException, ExecutionException{
		checkBlockReads("getItem", key);
		if(opening && countPendingReads(key) >= maxOpenReads){
			log.warn("Attempt to open too many reads on '{}' (sid {}, {})", new Object[]{key, null});
			throw new TooManyOpenReadsException();
		}
		if(log.isDebugEnabled()){
			  log.debug("get -> q={} t={} open={} peek={}", new Object[]{key, timeout, opening, peeking});
		}
		if(peeking){
			//  Stats.incr("cmd_peek")
		}else{
			// Stats.incr("cmd_get")
		}
		
		long startTime = System.currentTimeMillis();
		
		Promise<QItem> promise = queues.remove(key, timeout == null ? null : timeout.getTime(), opening, peeking, clientDesc);
//		waitingFor = promise;
		
		return promise;
		
	}
	
//	public void abortAnyOpenRead(){
//		cancelAllPendingReads();
//	}
	
	public Boolean setItem(String key, Integer flags, Date expiry, byte[] data) throws AvailabilityException, InvalidNameCharacterException, IOException, InterruptedException{
		checkBlockWrites("setItem", key);
		
		long startInNano = System.nanoTime();
		boolean result = queues.add(key, data, expiry == null? null : expiry.getTime(), System.currentTimeMillis(), null);
		long endInNano = System.nanoTime();
		return result;
	}
	
	public void flush(String key) throws AvailabilityException, IOException, InterruptedException, InvalidNameCharacterException{
		checkBlockReads("flush", key);
		queues.flush(key, null);
	}
	
	public void delete(String key) throws AvailabilityException{
		checkBlockReads("delete", key);
		queues.delete(key, null);
	}
	
	public void flushExpired(String key) throws AvailabilityException, IOException, InvalidNameCharacterException{
		checkBlockReads("flushExpired", key);
		queues.flushExpired(key, false, null);
	}
	
	public boolean safeCheckBlockReads(){
		return false;
	}
	public void checkBlockReads(String op, String key) throws AvailabilityException{
		if(safeCheckBlockReads()){
			throw new AvailabilityException(op);
		}
	}
	
	public void checkBlockWrites(String op, String key) throws AvailabilityException{
	    if (serverStatus.isBlockWrites()) {
	        log.debug("Blocking {} on '{}' (sid {}, {})", new Object[]{op, key, null});
	        throw new AvailabilityException(op);
	    }
	}
	
	
	
	
}
