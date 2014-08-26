package com.lenovo.ecs.eyas.exception;

import java.io.IOException;

public class BrokenItemException extends IOException {
	public Long lastValidPosition;
	
	public BrokenItemException(Long lastValidPosition,  Throwable cause){
		super(cause);
		this.lastValidPosition = lastValidPosition;
	}

	@Override
	public String toString() {
		return "BrokenItemException [lastValidPosition=" + lastValidPosition
				+ "]";
	}
	
	
	
}
