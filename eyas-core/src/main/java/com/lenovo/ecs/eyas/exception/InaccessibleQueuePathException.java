package com.lenovo.ecs.eyas.exception;

public class InaccessibleQueuePathException extends Exception{
	public InaccessibleQueuePathException(){
		super("Inaccessible queue path: Must be a directory and writable");
	}
}
