package com.lenovo.ecs.eyas;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

public class DeadlineWaitQueue<A> {
	private Timer timer;
	private LinkedHashSet<Waiter> queue = new LinkedHashSet<Waiter>();
	public DeadlineWaitQueue(Timer timer){
		this.timer = timer;
	}
	
	public abstract static class Waiter{
		TimerTask timerTask;
		PersistentQueue persistentQueue;
		Promise<QItem> promise;
		
		public Waiter(PersistentQueue queue, Promise<QItem> promise){
			this.persistentQueue = queue;
			this.promise = promise;
		}
		
		
		public Waiter(TimerTask timerTask, PersistentQueue queue, Promise<QItem> promise){
			this.timerTask = timerTask;
			this.persistentQueue = queue;
			this.promise = promise;
		}
		
		public abstract void awaken();
		public abstract void timeout();
		public TimerTask getTimerTask() {
			return timerTask;
		}
		public void setTimerTask(TimerTask timerTask) {
			this.timerTask = timerTask;
		}	
		
	}
	
	public Waiter add(long deadline, PersistentQueue persistentQueue, Promise<QItem> promise){
		final Waiter waiter = new Waiter(persistentQueue, promise){
			@Override
			public void awaken() {
			     if (promise.isCancelled()) {
			            promise.setValue(null);
			            persistentQueue.getWaiters().trigger();
		          } 
			}

			@Override
			public void timeout() {
			      promise.setValue(null);
			}
			
		};
		TimerTask timerTask = new TimerTask(){
			@Override
			public void run() {
				synchronized(queue){
					if(queue.remove(waiter))
						waiter.timeout();	
				}
			}
			
		};
		timer.schedule(timerTask, new Date(deadline));
		waiter.setTimerTask(timerTask);
		synchronized(queue){
			queue.add(waiter);
		}
		return waiter;
	}
	
	public void remove(Waiter waiter){
		synchronized(queue){
			queue.remove(waiter);
		}
		waiter.timerTask.cancel();
	}
	
	public void trigger(){
		Waiter waiter = null;
		synchronized(queue){
			if(queue != null){
				Iterator<Waiter> i = queue.iterator();
				if(i.hasNext()){
					waiter = i.next();
					queue.remove(waiter);
				}
			}
		}
		if(waiter != null){
			waiter.timerTask.cancel();
			waiter.awaken();
			
		}
	}
	
	public void triggerAll(){
		Waiter[] tmp = null;
		synchronized(queue){
			if(!queue.isEmpty()){
				tmp = new Waiter[queue.size()];
				queue.toArray(tmp);
				queue.clear();
			}
		}
		if(tmp != null)
		for(Waiter w : tmp){
			w.timerTask.cancel();
			w.awaken();
		}
	}
	
	public void evictAll(){
		Waiter[] tmp = null;
		synchronized(queue){
			if(!queue.isEmpty()){
				tmp = new Waiter[queue.size()];
				queue.toArray(tmp);
				queue.clear();
			}
		}
		if(tmp != null)
		for(Waiter w : tmp){
			w.timerTask.cancel();
			w.timeout();
		}
	}
	
	public int size(){
		synchronized(queue){
			return queue.size();
		}
	}
}
