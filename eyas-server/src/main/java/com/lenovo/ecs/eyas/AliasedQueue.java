package com.lenovo.ecs.eyas;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import com.lenovo.ecs.eyas.config.AliasConfig;
import com.lenovo.ecs.eyas.exception.InvalidNameCharacterException;

public class AliasedQueue {
	private String name;
	volatile private AliasConfig config;
	private QueueCollection queueCollection;
	private AtomicLong putItems = new AtomicLong(0);
	private AtomicLong putBytes = new AtomicLong(0);
	private Long createTime;
	
	
	
	public AliasedQueue(String name, AliasConfig config, QueueCollection queueCollection){
		this.name = name;
		this.config = config;
		this.queueCollection = queueCollection;
	}
	
	public String statNamed(String statName){
		return "q/"+name+"/"+statName;
	}
	
	
	
	public boolean add(byte[] value, Long expiry, Long addTime, String clientDesc) throws InvalidNameCharacterException, IOException, InterruptedException{
		putItems.getAndIncrement();
		putBytes.getAndAdd(value.length);
		boolean result = false;
		for(String queue : config.getDestinationQueues()){
			result = queueCollection.add(queue, value, expiry, addTime, clientDesc) & result;
		}
		return result;
	}

	public AliasConfig getConfig() {
		return config;
	}

	public void setConfig(AliasConfig config) {
		this.config = config;
	}
	
	
}
