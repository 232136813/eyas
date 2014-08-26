package com.lenovo.ecs.eyas;

@Deprecated
public abstract class MethodPack {
	public abstract void methodInEyasHandler(QItem item, Integer id);
	public abstract void methodInThriftHandler(QueueTransaction tran);
}
