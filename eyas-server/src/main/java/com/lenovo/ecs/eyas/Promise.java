package com.lenovo.ecs.eyas;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.lenovo.ecs.eyas.DeadlineWaitQueue.Waiter;
import com.lenovo.ecs.eyas.util.TimerAdapter;

public abstract class Promise<T> implements Future<T>{
	protected Waiter waiter;
	protected volatile T value;
	protected AtomicBoolean hasResultOrCanceled = new AtomicBoolean(false);
	protected volatile boolean isDone = false;
	protected volatile boolean isCanceled = false;
//	protected TimerAdapter timer;
	public abstract void onCancelation();
	
	public Promise(){
		
	}
	
	public boolean cancel(boolean mayInterruptIfRunning) {
		if(hasResultOrCanceled.compareAndSet(false, true)){
			onCancelation();
			isCanceled = true;
			isDone = true;
			return true;
		}
		return false;
	}

	public boolean isCancelled() {
		return isCanceled;
	}

	public boolean isDone() {
		return isDone;
	}

	public void setValue(T value){
		if(hasResultOrCanceled.compareAndSet(false, true)){
			this.value = value;
			this.isCanceled = false;
			this.isDone = true;
		}
	
	}
	public T get(){
		return value;
	}

	public T get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return value;
	}
	
	public void setWaiter(Waiter waiter){
		this.waiter = waiter;
	}
	
	

}
