package com.lenovo.ecs.eyas.config;


/*
 * 所有存储都是用byte作为单位
 * 所有时间都是用毫秒作为单位
 * @author songkun1
 *
 */
public class QueueConfig {
	
	private Integer maxItems;//单个queue最多的数据数量
	private Long maxSize;// 单个queue的最大占用存储空间大小   byte
	private Long maxItemSize;//最大的单个存储对象的大小  byte
	private Long maxAge;//对象的默认最大存活时间 单位毫秒
	private Long defaultJournalSize;//单个journal文件的大小  byte
	private Long maxMemorySize;//单个queue占用的最大内存大小 byte
	private Long maxJournalSize;//单个queue journal最大存储空间 byte
	private Boolean discardOldWhenFull;
	private Boolean keepJournal;
	private Long syncJournal;//多久刷一次磁盘  单位 millsec
	private String expireToQueue;
	private Integer maxExpireSweep;
	private Boolean fanoutOnly;
	private Long maxQueueAge; //最大的队列存活时间  单位毫秒
	
	
	
	
	
	public QueueConfig(){
	}
	
	public QueueConfig(Integer maxItems, Long maxSizeInByte, 
			Long maxItemSizeInByte,
			Long maxAgeInMillsec, Long defaultJournalSizeInByte, Long maxMemorySizeInByte,
			Long maxJournalSizeInByte, Boolean discardOldWhenFull,
			Boolean keepJournal, Long syncJournalInMillsec, String expireToQueue,
			Integer maxExpireSweep, Boolean fanoutOnly, Long maxQueueAgeInMillsec) {
		super();
		this.maxItems = maxItems;
		this.maxSize = maxSizeInByte;
		this.maxItemSize = maxItemSizeInByte;
		this.maxAge = maxAgeInMillsec;
		this.defaultJournalSize = defaultJournalSizeInByte;
		this.maxMemorySize = maxMemorySizeInByte;
		this.maxJournalSize = maxJournalSizeInByte;
		this.discardOldWhenFull = discardOldWhenFull;
		this.keepJournal = keepJournal;
		this.syncJournal = syncJournalInMillsec;
		this.expireToQueue = expireToQueue;
		this.maxExpireSweep = maxExpireSweep;
		this.fanoutOnly = fanoutOnly;
		this.maxQueueAge = maxQueueAgeInMillsec;
	}

	public QueueConfig(QueueConfig srcConfig){
		this(srcConfig.getMaxItems(), srcConfig.getMaxSize(), srcConfig.getMaxItemSize(), srcConfig.getMaxAge(), srcConfig.getDefaultJournalSize(),
				srcConfig.getMaxMemorySize(), srcConfig.getMaxJournalSize(), srcConfig.getDiscardOldWhenFull(),
				srcConfig.getKeepJournal(), srcConfig.getSyncJournal(), srcConfig.getExpireToQueue(),
				srcConfig.getMaxExpireSweep(), srcConfig.getFanoutOnly(), srcConfig.getMaxQueueAge());
		
		
	}
	
	public Integer getMaxItems() {
		return maxItems;
	}



	public void setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
	}



	public Long getMaxSize() {
		return maxSize;
	}



	public void setMaxSize(Long maxSize) {
		this.maxSize = maxSize;
	}



	public Long getMaxItemSize() {
		return maxItemSize;
	}



	public void setMaxItemSize(Long maxItemSize) {
		this.maxItemSize = maxItemSize;
	}



	public Long getMaxAge() {
		return maxAge;
	}



	public void setMaxAge(Long maxAge) {
		this.maxAge = maxAge;
	}



	public Long getDefaultJournalSize() {
		return defaultJournalSize;
	}



	public void setDefaultJournalSize(Long defaultJournalSize) {
		this.defaultJournalSize = defaultJournalSize;
	}



	public Long getMaxMemorySize() {
		return maxMemorySize;
	}



	public void setMaxMemorySize(Long maxMemorySize) {
		this.maxMemorySize = maxMemorySize;
	}



	public Long getMaxJournalSize() {
		return maxJournalSize;
	}



	public void setMaxJournalSize(Long maxJournalSize) {
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



	public Long getSyncJournal() {
		return syncJournal;
	}



	public void setSyncJournal(Long syncJournal) {
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



	public Long getMaxQueueAge() {
		return maxQueueAge;
	}



	public void setMaxQueueAge(Long maxQueueAge) {
		this.maxQueueAge = maxQueueAge;
	}



	@Override
	public String toString() {
		return "QueueConfig [maxItems=" + maxItems + ", maxSize=" + maxSize
				+ ", maxItemSize=" + maxItemSize + ", maxAge=" + maxAge
				+ ", defaultJournalSize=" + defaultJournalSize
				+ ", maxMemorySize=" + maxMemorySize + ", maxJournalSize="
				+ maxJournalSize + ", discardOldWhenFull=" + discardOldWhenFull
				+ ", keepJournal=" + keepJournal + ", syncJournal="
				+ syncJournal + ", expireToQueue=" + expireToQueue
				+ ", maxExpireSweep=" + maxExpireSweep + ", fanoutOnly="
				+ fanoutOnly + ", maxQueueAge=" + maxQueueAge + "]";
	}
	
	
	
}
