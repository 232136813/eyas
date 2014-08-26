package com.lenovo.ecs.eyas.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  
 * @author songkun1
 *
 */
public class AliasConfig {
	String name;
	private Set<String> destinationQueues = new HashSet<String>();
	
	public AliasConfig(){
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<String> getDestinationQueues() {
		return destinationQueues;
	}

	public void setDestinationQueues(Set<String> destinationQueues) {
		this.destinationQueues = destinationQueues;
	}

	@Override
	public String toString() {
		return "AliasConfig [name=" + name + ", destinationQueues="
				+ destinationQueues + "]";
	}



}
