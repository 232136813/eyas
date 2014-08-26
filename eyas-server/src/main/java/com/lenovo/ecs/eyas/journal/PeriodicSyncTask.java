package com.lenovo.ecs.eyas.journal;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class PeriodicSyncTask implements Runnable{
	
	volatile private ScheduledFuture<PeriodicSyncTask> scheduledFsync = null;
	private ScheduledExecutorService scheduler;
	private long initialDelay;
	private long period;
	
	
	/**
	 * 
	 * @param service
	 * @param initialDelay  second
	 * @param period   second
	 */
	public PeriodicSyncTask(ScheduledExecutorService scheduler, Long initialDelay, Long period){
		this.scheduler = scheduler;
		if(initialDelay != null)
			this.initialDelay = initialDelay.longValue();
		if(period != null)
			this.period = period.longValue();
	}

	public void start() {
	    synchronized (this){
	      if (scheduledFsync == null && period > 0L) {
	    	 scheduledFsync = (ScheduledFuture<PeriodicSyncTask>) scheduler.scheduleWithFixedDelay(this, initialDelay, period , TimeUnit.MILLISECONDS);
	      }
	    }
	}

	public void stop() {
	    synchronized(this) { 
	    	_stop(); 
	    }
	}

	public void stopIf(boolean stop) {
		synchronized(this) {
			if (stop) _stop();
		}
	}

	private void _stop() {
		if(scheduledFsync != null){
			scheduledFsync.cancel(false);
			scheduledFsync = null;
		}

	}
	
}
