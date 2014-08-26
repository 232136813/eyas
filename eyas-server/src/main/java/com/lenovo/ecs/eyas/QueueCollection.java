package com.lenovo.ecs.eyas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lenovo.ecs.eyas.config.AliasConfig;
import com.lenovo.ecs.eyas.config.AliasConfigBuilder;
import com.lenovo.ecs.eyas.config.QueueConfig;
import com.lenovo.ecs.eyas.config.QueueConfigBuilder;
import com.lenovo.ecs.eyas.exception.InaccessibleQueuePathException;
import com.lenovo.ecs.eyas.exception.InvalidNameCharacterException;
import com.lenovo.ecs.eyas.journal.Journal;
import com.lenovo.ecs.eyas.journal.JournalPackerTask;


public class QueueCollection {
	
	private static final Logger logger = LoggerFactory.getLogger(QueueCollection.class); 
	private String queueFolder;
	private File path;
	private Timer timer;
	private ScheduledExecutorService journalSyncScheduler;
	private JournalPackerTask journalPackerTask;
	final private Map<String, PersistentQueue> queues = new HashMap<String, PersistentQueue>();
	final private Map<String, HashSet<String>> fanout_queues = new HashMap<String, HashSet<String>>();
	final private Map<String, AliasedQueue> aliases = new HashMap<String, AliasedQueue>();
	
	volatile private QueueConfig defaultQueueConfig;
	volatile private List<QueueConfigBuilder> queueConfigBuilders;
	volatile private List<AliasConfigBuilder> aliasConfigBuilders;
	volatile private Map<String, QueueConfigBuilder> queueBuilderMap = new HashMap<String, QueueConfigBuilder>();
	volatile private Map<String, AliasConfig> aliasConfigMap =  new HashMap<String, AliasConfig>();
	
	volatile private Boolean shuttingDown = false; 
	
	
	
	
	public QueueCollection(String queueFolder, Timer timer, ScheduledExecutorService journalSyncScheduler,
			QueueConfig defaultQueueConfig, List<QueueConfigBuilder> queueConfigBuilders, List<AliasConfigBuilder> aliasConfigBuilders) throws InaccessibleQueuePathException{
		this.queueFolder = queueFolder;
		this.timer = timer;
		this.journalSyncScheduler = journalSyncScheduler;
		this.defaultQueueConfig = defaultQueueConfig;
		this.path = new File(queueFolder);
		if(this.path.isDirectory()){
			path.mkdirs();
		}
		
		if(!path.isDirectory() || !path.canWrite()){
			throw new InaccessibleQueuePathException();
		}
		this.queueConfigBuilders = queueConfigBuilders;
		if(queueConfigBuilders != null){
			for(QueueConfigBuilder e : queueConfigBuilders){
				this.queueBuilderMap.put(e.getName(), e);
			}
		}
		this.aliasConfigBuilders = aliasConfigBuilders;
		if(aliasConfigBuilders != null){
			for(AliasConfigBuilder e : aliasConfigBuilders){
				this.aliasConfigMap.put(e.getName(), e.build());
			}
		}
	}
	
	//检查 alias 与 queuename 有没有重复的
	private void checkNames(){
		TreeSet<String> duplicates = new TreeSet<String>();
		Set<String> queueNames = queues.keySet();
		for(String key : aliases.keySet()){
			if(queueNames.contains(key)){
				duplicates.add(key);
			}
		}
		if(!duplicates.isEmpty()){
			logger.warn("queue name(s) masked by alias(es): {}", duplicates.toString());
		}
	}
	
	private QueueConfig getQueueConfig(String name, String masterName){

		if(masterName != null){
			QueueConfig masterConfig = getQueueConfig(masterName, null);
			if(masterConfig == null)return new QueueConfig();
			return new QueueConfig(masterConfig);
		}else{
			QueueConfig config = null;
			QueueConfigBuilder builder = queueBuilderMap.get(name);
			if(builder != null){
				config = builder.build();	
			}
			if(config != null)return config;
			else return defaultQueueConfig;
		}
	}
	
	public PersistentQueue buildQueue(String name, String masterName, String path, String clientDesc) throws InvalidNameCharacterException, IOException{
		if(name.contains(".") || name.contains("/") || name.contains("~")){
			throw new InvalidNameCharacterException("QueueName contails Illegal characters name = "+name+".");
		}
		
		QueueConfig config = getQueueConfig(name, masterName);
		logger.info("Setting up queue {}: {} (via {})", new Object[]{name, config, clientDesc});
		return new PersistentQueue(name, queueFolder, config, timer, journalSyncScheduler, journalPackerTask, this);
		
	}
	
	
	public void loadQueues() throws Exception {
		  String startupDesc = "<startup>";
		  Set<String> qnames = Journal.getQueueNamesFromFolder(path);
		  for(String q : qnames){
			  queue(q, startupDesc);
		  }
		  createAliases();
	}
	
	public PersistentQueue queue(String name, String desc) throws InvalidNameCharacterException, IOException {
		return queue(name, true, desc);
	}
	
	public PersistentQueue queue(String name, boolean create) throws InvalidNameCharacterException, IOException {
		return queue(name, create, null);
	}
	
	public PersistentQueue queue(String name, boolean create, String clientDesc) throws InvalidNameCharacterException, IOException{
	    synchronized(this) {
	        if (shuttingDown) {
	        	return null;
	        } else if (create) {
	        	if(queues.get(name) != null){
	        		return queues.get(name);
	        	}else{
	        		PersistentQueue q = null;
	        		if(name.contains("+")){
	        			String master = name.split("+")[0];
	        			PersistentQueue fanoutQ = buildQueue(name, master, path.getPath(), clientDesc);
	        			HashSet<String> set = fanout_queues.get(master);
	        			if(set == null){
	        				set = new HashSet<String>();
	        			}
	        			set.add(name);
	        			q = fanoutQ;
	        		}else{
	        			q = buildQueue(name, null, path.getPath(), clientDesc);
	        		}
	        		q.setup();
	        		queues.put(name, q);
	        		return q;
	        		
	        	}
	        }else{
	        	return queues.get(name);
	        }
	        	
	    }
	}
	
	public synchronized void createAliases(){
		checkNames();
		for(String name : aliasConfigMap.keySet()){
			AliasConfig config = aliasConfigMap.get(name);
			AliasedQueue aq = aliases.get(name);
			if(aq != null){
				aq.setConfig(config);
			}else{
				AliasedQueue alias = new AliasedQueue(name, config, this);
		        aliases.put(name, alias);
			}
		}
	}
	

	
	public List<String> queueNames(boolean excludeAliases){
		ArrayList<String> list = new ArrayList<String>();
		synchronized(this){
			if(excludeAliases){
				list.addAll(queues.keySet());
			}else{
				list.addAll(queues.keySet());
				list.addAll(aliases.keySet());
			}
		}
		return list;
	}
	
	public List<String> queueNames(){
		return queueNames(false);
	}
	
	public int currentItems(){
		int currentItems = 0;
		for(PersistentQueue queue : queues.values()){
			currentItems += queue.length();
		}
		return currentItems;
	}
	
	public long currentBytes(){
		long currentBytes = 0;
		for(PersistentQueue queue : queues.values()){
			currentBytes += queue.bytes();
		}
		return currentBytes;
	}
	
    public double reservedMemoryRatio(){
		double maxBytes = 0;
		for(PersistentQueue queue : queues.values()){
			maxBytes += queue.maxMemoryBytes();
		}
		double systemMaxHeapBytes = Runtime.getRuntime().maxMemory();
		return maxBytes/systemMaxHeapBytes;
	}
	
	
	
	public void reload(QueueConfig newDefaultQueueConfig, List<QueueConfigBuilder> newQueueBuilders, List<AliasConfigBuilder> newAliasBuilders){
		defaultQueueConfig = newDefaultQueueConfig;
		queueConfigBuilders = newQueueBuilders;
		queueBuilderMap = new HashMap<String, QueueConfigBuilder>();
		for(QueueConfigBuilder qb : queueConfigBuilders){
			queueBuilderMap.put(qb.getName(), qb);
		}
		for(String name : queues.keySet()){
			PersistentQueue queue = queues.get(name);
			String masterName = null;
			if(name.contains("+")) masterName =  name.split("+")[0];
			QueueConfig config = getQueueConfig(name, masterName);
			queue.setConfig(config);
		}
		
		aliasConfigBuilders = newAliasBuilders;
		aliasConfigMap = new HashMap<String, AliasConfig>();
		
		for(AliasConfigBuilder ab : aliasConfigBuilders){
			aliasConfigMap.put(ab.getName(), ab.build());
		}
		 createAliases();
	}

	public AliasedQueue alias(String name){
		synchronized(this){
			if(shuttingDown)
				return null;
			else
				return aliases.get(name);
		}
	}
	
	public boolean add(String key, byte[] item, Long expiry, Long addTime, String clientDesc) throws InvalidNameCharacterException, IOException, InterruptedException  {
		AliasedQueue aq = alias(key);
		if(aq != null)
			return aq.add(item, expiry, addTime, clientDesc);
		else{
			HashSet<String> fanouts = fanout_queues.get(key);
			if(fanouts != null)
				for(String name : fanouts){
					add(name, item, expiry, addTime, clientDesc);
				}
			
			PersistentQueue queue = queue(key, clientDesc);
			if(queue == null){
				return false;
			}else{
				boolean result = queue.add(item, expiry, null, addTime);
				return result;
			}
		}

	
	}
	
	public boolean add(String key, byte[] item) throws InvalidNameCharacterException, IOException, InterruptedException {
		return add(key, item, null, System.currentTimeMillis(), null);
	}
	
	public boolean add(String key, byte[] item, Long expiry) throws InvalidNameCharacterException, IOException, InterruptedException {
		return add(key, item, expiry, System.currentTimeMillis(), null);
	}	

	
	public Promise<QItem> remove(String key, Long deadline, boolean transaction, boolean peek, String clientDesc) throws InvalidNameCharacterException, IOException, InterruptedException, ExecutionException {
		if(alias(key) != null){
			Promise promise = new Promise(){
				@Override
				public void onCancelation() {}
			};
			promise.setValue(null);
			return promise;
		}
		PersistentQueue queue = queue(key, clientDesc);
		if(queue == null){
			Promise<QItem> promise = new Promise<QItem>(){
				@Override
				public void onCancelation() {}
			};
			promise.setValue(null);
			return promise;
		}else{
			Promise<QItem> promise = null;
			if(peek){
				promise = queue.waitPeek(deadline);
			}else{
				promise = queue.waitRemove(deadline, transaction);
			}
			QItem item = promise.get();
			if(item == null){
				//get miss
			}else{
				//get hit
			}
			return promise;
		}
	}
	
	public void unremove(String key, Integer xid) throws InvalidNameCharacterException, IOException{
		PersistentQueue queue = queue(key, false);
		if(queue != null){
			queue.unremove(xid);
		}
	}
	
	public void confirmRemove(String key, Integer xid) throws InvalidNameCharacterException, IOException{
		PersistentQueue queue = queue(key, false);
		if(queue != null){
			queue.confirmRemove(xid);
		}
		
	}
	
	public void flush(String key, String clientDesc) throws IOException, InterruptedException, InvalidNameCharacterException{
		PersistentQueue queue = queue(key, false);
		if(queue != null){
			queue.flush();
		}
	}
	
	public void delete(String name, String clientDesc){
		synchronized(this){
			if(!shuttingDown){
				PersistentQueue queue = queues.get(name);
				if(queue != null){
					queue.close();
					queue.destroyJournal();
					//queue.removeStats();
					//Stats.incr("queue_deletes");
					logger.info("Queue {} deleted by {}", name, clientDesc);
				}
				
				if(name.contains("+")){
					String master = name.split("+")[0];
					HashSet set = fanout_queues.get(master);
					if(set == null){
						set = new HashSet();
						fanout_queues.put(master, set);
					}
					set.remove(name);
					logger.info("Fanout queue {} dropped from {} by {}", new Object[]{name, master, clientDesc});
				}
			}
		}
	}
	
	public Integer flushExpired(String name, boolean limit, String clientDesc) throws IOException, InvalidNameCharacterException {
		if(shuttingDown)return 0;
		else{
			PersistentQueue queue = queue(name, false);
			Integer flushed = null;
			if(queue != null){
				flushed = queue.discardExpired(limit);
		        if (flushed != null && flushed > 0) {
		            logger.info("Queue {} flushed of {} expired item(s) by {}",
		                     new Object[]{name, flushed, clientDesc});
		          }
			}
			if(flushed == null)return 0;
			return flushed;
			
		}
	}
	
	public void expireQueue(String name){
		synchronized(this){
			if(!shuttingDown){
				PersistentQueue queue = queues.get(name);
				if(queue != null && queue.isReadyForExpiration()){
					delete(name, null);
				}
				logger.info("Expired queue {}", name);
			}
		}
	}
	
	public Integer flushAllExpired(boolean limit, String clientDesc) throws IOException, InvalidNameCharacterException  {
		List<String> names = queueNames(true);
		Integer sum = 0;
		for(String name : names){
			Integer num = flushExpired(name, limit, clientDesc);
			if(num != null)
				sum += num;
		}
		return sum;
		
	}
	
	public void deleteExpiredQueues(){
		List<String> names = queueNames(true);
		if(names != null){
			for(String name : names){
				expireQueue(name);
			}
		}
	}
	
	public void deleteExpiredQueue(){
		List<String> names = queueNames(true);
		for(String name : names){
			expireQueue(name);
		}
	}
	
	public void evictWaiters(){
		synchronized(this){
			if(shuttingDown){
				return;
			}
			for(Entry<String, PersistentQueue> e: queues.entrySet()){
				PersistentQueue q = e.getValue();
				if(q != null){
					q.evictWaiters();
				}
			}
		}
	}
	
	
	
	public void shutDown(){
		synchronized(this){
		    if (shuttingDown) {
		        return;
		      }
		    shuttingDown = true;
			for(Entry<String, PersistentQueue> e: queues.entrySet()){
				PersistentQueue q = e.getValue();
				if(q != null){
					q.close();
				}
			}
		    queues.clear();
		}
	}
	
	
	
}
