package com.lenovo.ecs.eyas.test;

import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.util.HashedWheelTimer;

import com.lenovo.ecs.eyas.PersistentQueue;
import com.lenovo.ecs.eyas.QueueCollection;
import com.lenovo.ecs.eyas.config.QueueConfig;
import com.lenovo.ecs.eyas.config.QueueConfigBuilder;
import com.lenovo.ecs.eyas.exception.InvalidNameCharacterException;
import com.lenovo.ecs.eyas.journal.JournalPackerTask;
import com.lenovo.ecs.eyas.util.TimerAdapter;

public class PersistentQueueTest {
	/**
	 * @param args
	 * @throws InvalidNameCharacterException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InvalidNameCharacterException, IOException, InterruptedException {
		QueueConfigBuilder builder = new QueueConfigBuilder();
		//String name, String persistentPath, QueueConfig config, Timer timer, ScheduledExecutorService journalSyncScheduler, JournalPackerTask packer, QueueCollection queueCollection
		HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(1024*10, TimeUnit.MICROSECONDS);
		Timer timer = new TimerAdapter(hashedWheelTimer);
		ScheduledExecutorService service = new ScheduledThreadPoolExecutor(2);
		PersistentQueue queue = new PersistentQueue("abc", "d:/var/spool", builder.build(), timer, service, null, null);
		queue.setup();
		for(int i=0; i<100000;i++){
			queue.add(new byte[1024]);
		}
		long start = System.currentTimeMillis();
			for(int j=0;j<100000;j++){
				queue.remove();
			}
		long end = System.currentTimeMillis();
		System.out.println("timer = " + (end - start));
	}
}
