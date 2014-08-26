package com.lenovo.ecs.eyas.util;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.print.attribute.Attribute;

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;


public class TimerAdapter extends Timer{

	private HashedWheelTimer timer;
	public TimerAdapter(HashedWheelTimer timer){
		super("timer", true);
		this.timer = timer;
	}
	

	
	@Override
	public void schedule(final TimerTask task, long delay) {
//		super.schedule(task, delay);
		org.jboss.netty.util.TimerTask taskAdapter = new org.jboss.netty.util.TimerTask(){
			public void run(Timeout timeout) throws Exception {
				task.run();
			}
		};
		timer.newTimeout(taskAdapter, delay, TimeUnit.MILLISECONDS);
	}

	@Override
	public void schedule(final TimerTask task, Date time) {
		if(time == null || time.getTime() < System.currentTimeMillis())
			throw new IllegalArgumentException("Date is invalid!");
		schedule(task, time.getTime() - System.currentTimeMillis());
	}

	@Override
	public void schedule(final TimerTask task, long delay, long period) {
		throw new UnsupportedOperationException ();
	}

	@Override
	public void schedule(TimerTask task, Date firstTime, long period) {
		throw new UnsupportedOperationException ();
	}

	@Override
	public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
		throw new UnsupportedOperationException ();
	}

	@Override
	public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
		throw new UnsupportedOperationException ();
	}

	@Override
	public void cancel() {
		timer.stop();
	}



}
