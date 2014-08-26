package com.lenovo.ecs.eyas.exception;

public class InvalidNameCharacterException extends Exception{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String cause;
	
	public InvalidNameCharacterException(String cause){
		this.cause = cause;
	}
	public InvalidNameCharacterException(String cause, Throwable throwable){
		super(throwable);
		this.cause = cause;
	}
	
	@Override
	public String toString() {
		return "InvalidNameCharacterException [cause=" + cause + "]";
	}
}
