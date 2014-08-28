package com.lenovo.ecs.eyas.config;

import com.lenovo.ecs.eyas.util.StorageUnit;
import com.lenovo.ecs.eyas.util.TimeUnitExt;


public class QueueConfigBuilder {
	
	
	private String name;
	private Integer maxItems;//单个queue最多的数据数量
	private String maxSize;// 单个queue的最大占用存储空间大小   
	private String maxItemSize;//最大的单个存储对象的大小  
	private String maxAge;//对象的默认最大存活时间 单位毫秒
	private String defaultJournalSize;//单个journal文件的大小  
	private String maxMemorySize;//单个queue占用的最大内存大小 
	private String maxJournalSize;//单个queue journal最大存储空间 
	private Boolean discardOldWhenFull;
	private Boolean keepJournal;
	private String syncJournal;//多久刷一次磁盘  
	private String expireToQueue;
	private Integer maxExpireSweep;
	private Boolean fanoutOnly;
	private String maxQueueAge; //最大的队列存活时间
	
	
	public static final long _defaultJournalSize = StorageUnit.MEGABYTE.toByte(128);
	public static final long _maxMemorySize = StorageUnit.MEGABYTE.toByte(128);
	public static final long _maxJournalSize =  StorageUnit.GIGABYTE.toByte(1);
	public static final boolean _keepJournal = true; 
	
	public QueueConfigBuilder(){
		
	}

	public QueueConfig build(){
		QueueConfig config = new QueueConfig();
		if(maxItems != null){
			config.setMaxItems(maxItems);
		}else{
			config.setMaxItems(Integer.MAX_VALUE);
		}
		if(maxSize != null){
			config.setMaxSize(StorageUnit.getStorageInBytes(maxSize));
		}else{
			config.setMaxSize(Long.MAX_VALUE);
		}
		
		if(maxItemSize != null){
			config.setMaxItemSize(StorageUnit.getStorageInBytes(maxItemSize));
		}else{
			config.setMaxItemSize(Long.MAX_VALUE);
		}
		
		config.setMaxAge(TimeUnitExt.getTimeInMillSec(maxAge));//maxAge == null is default
		
		
		if(defaultJournalSize != null){
			config.setDefaultJournalSize(StorageUnit.getStorageInBytes(defaultJournalSize));
		}else{
			if(EyasConfigLoader.getInstance().getEyasConfig().getDefaultJournalSize() != null){
				config.setDefaultJournalSize(EyasConfigLoader.getInstance().getEyasConfig().getDefaultJournalSize());
			}else{
				config.setDefaultJournalSize(_defaultJournalSize);
			}
		}
		if(maxMemorySize != null){
			config.setMaxMemorySize(StorageUnit.getStorageInBytes(maxMemorySize));
			System.out.println(name +" maxMemorySize = " +config.getMaxMemorySize() + "; " + StorageUnit.getStorageInBytes(maxMemorySize));
		}else{
			System.out.println(name + " maxMemorySize2 = " +config.getMaxMemorySize() + "; " + EyasConfigLoader.getInstance().getEyasConfig().getMaxMemorySize());
			if(EyasConfigLoader.getInstance().getEyasConfig().getMaxMemorySize() != null){
				config.setMaxMemorySize(EyasConfigLoader.getInstance().getEyasConfig().getMaxMemorySize());
			}else{
				config.setMaxMemorySize(_maxMemorySize);
			}
		}
		
		if(maxJournalSize != null){
			config.setMaxJournalSize(StorageUnit.getStorageInBytes(maxJournalSize));
		}else{
			if(EyasConfigLoader.getInstance().getEyasConfig().getMaxJournalSize() != null){
				config.setMaxJournalSize(EyasConfigLoader.getInstance().getEyasConfig().getMaxJournalSize());
			}else{
				config.setMaxJournalSize(_maxJournalSize);
			}
		}
		
		if(keepJournal != null){
			config.setKeepJournal(keepJournal);
		}else{
			config.setKeepJournal(_keepJournal);
		}
	
		if(discardOldWhenFull != null){
			config.setDiscardOldWhenFull(discardOldWhenFull);
		}else{
			config.setDiscardOldWhenFull(false);
		}
		
		config.setSyncJournal(TimeUnitExt.getTimeInMillSec(syncJournal));
		config.setExpireToQueue(expireToQueue);
		config.setMaxExpireSweep(maxExpireSweep);
		if(fanoutOnly != null){
			config.setFanoutOnly(fanoutOnly);
		}else{
			config.setFanoutOnly(false);
		}
		config.setMaxQueueAge(TimeUnitExt.getTimeInMillSec(maxQueueAge));
		return config;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getMaxItems() {
		return maxItems;
	}

	public void setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
	}

	public String getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(String maxSize) {
		this.maxSize = maxSize;
	}

	public String getMaxItemSize() {
		return maxItemSize;
	}

	public void setMaxItemSize(String maxItemSize) {
		this.maxItemSize = maxItemSize;
	}

	public String getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(String maxAge) {
		this.maxAge = maxAge;
	}

	public String getDefaultJournalSize() {
		return defaultJournalSize;
	}

	public void setDefaultJournalSize(String defaultJournalSize) {
		this.defaultJournalSize = defaultJournalSize;
	}

	public String getMaxMemorySize() {
		return maxMemorySize;
	}

	public void setMaxMemorySize(String maxMemorySize) {
		this.maxMemorySize = maxMemorySize;
	}

	public String getMaxJournalSize() {
		return maxJournalSize;
	}

	public void setMaxJournalSize(String maxJournalSize) {
		this.maxJournalSize = maxJournalSize;
	}

	public Boolean getDiscardOldWhenFull() {
		return discardOldWhenFull;
	}

	public void setDiscardOldWhenFull(Boolean discardOldWhenFull) {
		this.discardOldWhenFull = discardOldWhenFull;
	}

	public Boolean getKeepJournal() {
		return keepJournal;
	}

	public void setKeepJournal(Boolean keepJournal) {
		this.keepJournal = keepJournal;
	}

	public String getSyncJournal() {
		return syncJournal;
	}

	public void setSyncJournal(String syncJournal) {
		this.syncJournal = syncJournal;
	}

	public String getExpireToQueue() {
		return expireToQueue;
	}

	public void setExpireToQueue(String expireToQueue) {
		this.expireToQueue = expireToQueue;
	}

	public Integer getMaxExpireSweep() {
		return maxExpireSweep;
	}

	public void setMaxExpireSweep(Integer maxExpireSweep) {
		this.maxExpireSweep = maxExpireSweep;
	}

	public Boolean getFanoutOnly() {
		return fanoutOnly;
	}

	public void setFanoutOnly(Boolean fanoutOnly) {
		this.fanoutOnly = fanoutOnly;
	}

	public String getMaxQueueAge() {
		return maxQueueAge;
	}

	public void setMaxQueueAge(String maxQueueAge) {
		this.maxQueueAge = maxQueueAge;
	}

	@Override
	public String toString() {
		return "QueueConfigBuilder [name=" + name + ", maxItems=" + maxItems
				+ ", maxSize=" + maxSize + ", maxItemSize=" + maxItemSize
				+ ", maxAge=" + maxAge + ", defaultJournalSize="
				+ defaultJournalSize + ", maxMemorySize=" + maxMemorySize
				+ ", maxJournalSize=" + maxJournalSize
				+ ", discardOldWhenFull=" + discardOldWhenFull
				+ ", keepJournal=" + keepJournal + ", syncJournal="
				+ syncJournal + ", expireToQueue=" + expireToQueue
				+ ", maxExpireSweep=" + maxExpireSweep + ", fanoutOnly="
				+ fanoutOnly + ", maxQueueAge=" + maxQueueAge + "]";
	}




	
	
	
}
