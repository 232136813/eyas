package com.lenovo.ecs.eyas.journal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;


import com.lenovo.ecs.eyas.Promise;

public class PeriodicSyncFile {
	private Long period;
	private FileChannel writer;
	private AtomicBoolean closed = new AtomicBoolean(false);
	
	private ConcurrentLinkedQueue<TimestampedPromise> promises = new ConcurrentLinkedQueue<TimestampedPromise>();
	private PeriodicSyncTask periodicSyncTask;

	public PeriodicSyncFile(File file, ScheduledExecutorService scheduler, Long period) throws FileNotFoundException{
		this.period = period;
		this.writer = new FileOutputStream(file, true).getChannel();
		this.periodicSyncTask = new PeriodicSyncTask(scheduler, period, period) {
		    public void run() {
		      if (!closed.get() && !promises.isEmpty()) fsync();
		    }
		};
	}
	
	private void fsync() {
	    synchronized (this){
	      int completed = promises.size();
	      try {
	        writer.force(false);
	      } catch (IOException e){
	          for (int i=0; i< completed; i++) {
	            promises.poll().setValue(e);
	          }
	        return;
	      }

	      for (int i=0; i< completed; i++) {
	    	  TimestampedPromise promise = promises.poll();
	    	  promise.setValue(null);
	      }
	      periodicSyncTask.stopIf(promises.isEmpty());
	    }
	}
	
	 public TimestampedPromise write(ByteBuffer buffer){
		   do {
		      try {
				writer.write(buffer);
		      } catch (IOException e) {
		    	  e.printStackTrace();
		    	  TimestampedPromise promise = new TimestampedPromise(System.currentTimeMillis());
		    	  promise.setValue(e);
		          return promise;
			  }
		    } while (buffer.position() < buffer.limit());
		    if (period == null || period.intValue() < 0 || period==Long.MAX_VALUE) {//不设值或者小于0 或者 等于最大值 都不刷盘 
		     	TimestampedPromise promise = new TimestampedPromise(System.currentTimeMillis());
		     	promise.setValue(null);
		     	return promise;
		    	
		    }else if( period.intValue() == 0){//立刻刷盘
		      try {
		        writer.force(false);
		     	TimestampedPromise promise = new TimestampedPromise(System.currentTimeMillis());
		     	promise.setValue(null);
		     	return promise;
		      } catch (Exception e){
		    	  TimestampedPromise promise = new TimestampedPromise(System.currentTimeMillis());
		          promise.setValue(e);
		          return promise;
		      }
		    } else {
		      TimestampedPromise promise = new TimestampedPromise(System.currentTimeMillis());
		      promises.add(promise);
		      periodicSyncTask.start();
		      return promise;
		    }
	 }

	  public void close() throws IOException {
		    closed.set(true);
		    periodicSyncTask.stop();
		    fsync();
		    writer.close();
		    writer = null;
	 }

	  public Long position() throws IOException{
		  if(writer != null)
			  return writer.position();
		  return null;
	  }
	public static class TimestampedPromise extends Promise<Exception>{
		private Long time;

		public TimestampedPromise(Long time){
			this.time = time;
		}
		public Long getTime(){
			return time;
		}

		@Override
		public void onCancelation() {
			
		}

		
	}
	
	
	
}
