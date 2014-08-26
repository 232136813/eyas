package com.lenovo.ecs.eyas.config;

import java.util.HashSet;
import java.util.Set;



public class AliasConfigBuilder {
	private String name;
	private String destinationQueue;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDestinationQueue() {
		return destinationQueue;
	}
	public void setDestinationQueue(String destinationQueue) {
		this.destinationQueue = destinationQueue;
	}
	
	public AliasConfig build(){
		AliasConfig config = new AliasConfig();
		config.setName(name);
		config.setDestinationQueues(parseQueue(destinationQueue));
		return config;
	}
	
	private Set parseQueue(String destinationQueue){
		HashSet set = new HashSet();
		if(destinationQueue != null){
			String[] desQs = destinationQueue.split(",");
			for(String des : desQs){
				set.add(des);
			}
		}
		return set;
		
	}
	@Override
	public String toString() {
		return "AliasConfigBuilder [name=" + name + ", destinationQueue="
				+ destinationQueue + "]";
	}
	
	
}
