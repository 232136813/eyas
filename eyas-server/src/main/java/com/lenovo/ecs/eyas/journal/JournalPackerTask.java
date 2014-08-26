package com.lenovo.ecs.eyas.journal;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalPackerTask {
	private final static Logger logger = LoggerFactory.getLogger(JournalPackerTask.class);
	private final Queue<PackRequest> queue = new LinkedBlockingQueue<PackRequest>();
	private Thread thread = null;

	public void start() {
		synchronized (this){
			if (thread == null) {
				thread = new Thread("journal-packer") {
					public void run() {
						boolean running = true;
						while (running) {
							PackRequest requestOpt = queue.poll();
							if(requestOpt != null){
								pack(requestOpt);
							}else{
								running = false;
							}
						}
						logger.info("journal-packer exited.");
					}
				};
				thread.setDaemon(true);
				thread.start();
			} else {
				  logger.error("journal-packer already started.");
			}
		}
	}

	  public void shutdown() {
		  synchronized(this) {
			  if (thread != null) {
				  logger.info("journal-packer exiting.");
				  queue.add(null);
				  try {
					  thread.join(5000L);
				  } catch (InterruptedException e) {
					  e.printStackTrace();
				  }
				  thread = null;
			  } else {
				  logger.error("journal-packer not running.");
			  }
		  }
	  }

	  public void add(PackRequest request) {
		  queue.add(request);
	  }
	  private void pack(PackRequest request){
		  try {
		      	request.getJournal().pack(request);
		      	request.getJournal().getOutstandingPackRequests().decrementAndGet();
		  } catch (Throwable e){
		        logger.error("Uncaught exception in journal-packer: %s", e);
		  }
	  }

}