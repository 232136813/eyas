package com.lenovo.ecs.eyas;

import java.util.TimerTask;

@Deprecated
public class QueueTransaction {
	private String name;
	private Integer xid;
	private TimerTask timerTask;
	
	public QueueTransaction(String name, Integer xid, TimerTask timerTask){
		this.name = name;
		this.xid = xid;
		this.timerTask = timerTask;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getXid() {
		return xid;
	}

	public void setXid(Integer xid) {
		this.xid = xid;
	}

	public TimerTask getTimerTask() {
		return timerTask;
	}

	public void setTimerTask(TimerTask timerTask) {
		this.timerTask = timerTask;
	}
	
	
	
}
