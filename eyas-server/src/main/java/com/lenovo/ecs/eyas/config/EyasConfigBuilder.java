package com.lenovo.ecs.eyas.config;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.lenovo.ecs.eyas.util.StorageUnit;
import com.lenovo.ecs.eyas.util.TimeUnitExt;

/**
 * 
 * @author songkun1
 *
 */
public class EyasConfigBuilder {

	private String listenAddress;
	private int thriftListenPort;
	private String queuePath;
	private String clientTimeOut;
	private int maxOpenTransactions;
	private int expirationTimerFrequency;
	private String defaultJournalSize;
	private String maxMemorySize;
	private String maxJournalSize;
	private String syncJournal;
	
	private Map<String, QueueConfigBuilder> queueConfigsBuilders = new HashMap<String, QueueConfigBuilder>();
	private Map<String, AliasConfigBuilder> aliasConfigsBuilders = new HashMap<String, AliasConfigBuilder>();
	
	public EyasConfigBuilder(){

	}

	public EyasConfig build(){
		EyasConfig config = new EyasConfig();
		config.setListenAddress(listenAddress);
		config.setThriftListenPort(thriftListenPort);
		config.setQueuePath(queuePath);
		config.setClientTimeOut(TimeUnitExt.getTimeInMillSec(clientTimeOut));
		config.setMaxOpenTransactions(maxOpenTransactions);
		config.setExpirationTimerFrequency(expirationTimerFrequency);
		config.setDefaultJournalSize(StorageUnit.getStorageInBytes(defaultJournalSize));
		config.setMaxJournalSize(StorageUnit.getStorageInBytes(maxJournalSize));
		config.setSyncJournal(TimeUnitExt.getTimeInMillSec(clientTimeOut));
		config.setMaxMemorySize(StorageUnit.getStorageInBytes(maxMemorySize));
		return config;
	}
	
	public void addQueueConfig(QueueConfigBuilder builder){
		queueConfigsBuilders.put(builder.getName(), builder);
	}
	
	public void addAliasConfig(AliasConfigBuilder builder){
		aliasConfigsBuilders.put(builder.getName(), builder);
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

	public String getClientTimeOut() {
		return clientTimeOut;
	}

	public void setClientTimeOut(String clientTimeOut) {
		this.clientTimeOut = clientTimeOut;
	}

	public int getMaxOpenTransactions() {
		return maxOpenTransactions;
	}

	public void setMaxOpenTransactions(int maxOpenTransactions) {
		this.maxOpenTransactions = maxOpenTransactions;
	}

	public int getExpirationTimerFrequency() {
		return expirationTimerFrequency;
	}

	public void setExpirationTimerFrequency(int expirationTimerFrequency) {
		this.expirationTimerFrequency = expirationTimerFrequency;
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

	public String getSyncJournal() {
		return syncJournal;
	}

	public void setSyncJournal(String syncJournal) {
		this.syncJournal = syncJournal;
	}


	

	public Map<String, QueueConfigBuilder> getQueueConfigsBuilders() {
		return queueConfigsBuilders;
	}

	public void setQueueConfigsBuilders(
			Map<String, QueueConfigBuilder> queueConfigsBuilders) {
		this.queueConfigsBuilders = queueConfigsBuilders;
	}

	public Map<String, AliasConfigBuilder> getAliasConfigsBuilders() {
		return aliasConfigsBuilders;
	}

	public void setAliasConfigsBuilders(
			Map<String, AliasConfigBuilder> aliasConfigsBuilders) {
		this.aliasConfigsBuilders = aliasConfigsBuilders;
	}

	@Override
	public String toString() {
		return "EyasConfigBuilder [listenAddress=" + listenAddress
				+ ", thriftListenPort=" + thriftListenPort + ", queuePath="
				+ queuePath + ", clientTimeOut=" + clientTimeOut
				+ ", maxOpenTransactions=" + maxOpenTransactions
				+ ", expirationTimerFrequency=" + expirationTimerFrequency
				+ ", defaultJournalSize=" + defaultJournalSize
				+ ", maxMemorySize=" + maxMemorySize + ", maxJournalSize="
				+ maxJournalSize + ", syncJournal=" + syncJournal
				+", queueConfigsBuilders="
				+ queueConfigsBuilders + ", aliasConfigsBuilders="
				+ aliasConfigsBuilders + "]";
	}


    
    
    
	
}
