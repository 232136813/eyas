package com.lenovo.ecs.eyas;

public class ServerStatus {
	boolean blockReads;
	boolean blockWrites;
	
	
	public boolean isBlockReads() {
		return blockReads;
	}
	public void setBlockReads(boolean blockReads) {
		this.blockReads = blockReads;
	}
	public boolean isBlockWrites() {
		return blockWrites;
	}
	public void setBlockWrites(boolean blockWrites) {
		this.blockWrites = blockWrites;
	}
	
	
	
}
