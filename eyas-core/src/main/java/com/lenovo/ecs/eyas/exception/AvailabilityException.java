package com.lenovo.ecs.eyas.exception;

public class AvailabilityException extends Exception{
	String cause;
	public AvailabilityException(String cause){
		this.cause = cause;
	}
}
