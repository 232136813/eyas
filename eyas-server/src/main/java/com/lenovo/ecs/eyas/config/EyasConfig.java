package com.lenovo.ecs.eyas.config;


import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author songkun1
 *
 */
public class EyasConfig {

	private String listenAddress;
	private int thriftListenPort;
	private String queuePath;
	private Long clientTimeOut;
	private Integer maxOpenTransactions;
	private Integer expirationTimerFrequency;
	private Long defaultJournalSize;
	private Long maxMemorySize;
	private Long maxJournalSize;
	private Long syncJournal;
	
	public EyasConfig(){

	}


	public String getListenAddress() {
		return listenAddress;
	}

	public void setListenAddress(String listenAddress) {
		this.listenAddress = listenAddress;
	}

	public int getThriftListenPort() {
		return thriftListenPort;
	}

	public void setThriftListenPort(int thriftListenPort) {
		this.thriftListenPort = thriftListenPort;
	}

	public String getQueuePath() {
		return queuePath;
	}

	public void setQueuePath(String queuePath) {
		this.queuePath = queuePath;
	}

	public Long getClientTimeOut() {
		return clientTimeOut;
	}

	public void setClientTimeOut(Long clientTimeOut) {
		this.clientTimeOut = clientTimeOut;
	}

	public Integer getMaxOpenTransactions() {
		return maxOpenTransactions;
	}

	public void setMaxOpenTransactions(Integer maxOpenTransactions) {
		this.maxOpenTransactions = maxOpenTransactions;
	}

	public Integer getExpirationTimerFrequency() {
		return expirationTimerFrequency;
	}

	public void setExpirationTimerFrequency(Integer expirationTimerFrequency) {
		this.expirationTimerFrequency = expirationTimerFrequency;
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

	public Long getSyncJournal() {
		return syncJournal;
	}

	public void setSyncJournal(Long syncJournal) {
		this.syncJournal = syncJournal;
	}


	@Override
	public String toString() {
		return "EyasConfig [listenAddress=" + listenAddress
				+ ", thriftListenPort=" + thriftListenPort + ", queuePath="
				+ queuePath + ", clientTimeOut=" + clientTimeOut
				+ ", maxOpenTransactions=" + maxOpenTransactions
				+ ", expirationTimerFrequency=" + expirationTimerFrequency
				+ ", defaultJournalSize=" + defaultJournalSize
				+ ", maxMemorySize=" + maxMemorySize + ", maxJournalSize="
				+ maxJournalSize + ", syncJournal=" + syncJournal
				+  "]";
	}


    
    
    
	
}
