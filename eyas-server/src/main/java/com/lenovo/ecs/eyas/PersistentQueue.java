package com.lenovo.ecs.eyas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lenovo.ecs.eyas.DeadlineWaitQueue.Waiter;
import com.lenovo.ecs.eyas.config.QueueConfig;
import com.lenovo.ecs.eyas.exception.InvalidNameCharacterException;
import com.lenovo.ecs.eyas.journal.Journal;
import com.lenovo.ecs.eyas.journal.JournalItem;
import com.lenovo.ecs.eyas.journal.JournalPackerTask;


public class PersistentQueue {
	
	private static final Logger log = LoggerFactory.getLogger(PersistentQueue.class);
	private static final Logger logger = LoggerFactory.getLogger("tmp");
	String name;
	String persistentPath;
	private volatile QueueConfig config;
	private Deque<QItem> queue = new ArrayDeque<QItem>();
	private boolean isFanout = false;
	private long queueSize = 0L;
	private long currentAge = 0L;
	private long queueLength = 0L;
	private long memoryBytes = 0L;
	private boolean closed = false;
	private boolean paused = false;
	private Journal journal = null;
	private Integer xidCounter = 0;
	private AtomicInteger totalDiscarded = new AtomicInteger(0);
	private AtomicInteger putItems = new AtomicInteger(0);
	private AtomicLong putBytes = new AtomicLong(0);
	private AtomicInteger totalExpired = new AtomicInteger(0);
	private AtomicInteger totalTransactions = new AtomicInteger(0);
	private AtomicInteger totalCanceledTransactions = new AtomicInteger(0);
	private TreeMap<Integer, QItem> openTransactions = new TreeMap<Integer, QItem>();
//	private QueueCollection queueCollection;
	private PersistentQueue expireQueue;
	private Timer timer;
	private final DeadlineWaitQueue<Waiter> waiters;
	private AtomicLong totalFlushes = new AtomicLong(0L);
	private Long _memoryBytes = 0l;
	private long createTime = 0L;

	public PersistentQueue(String name, String persistentPath, QueueConfig config, Timer timer, ScheduledExecutorService journalSyncScheduler, JournalPackerTask packer, QueueCollection queueCollection) throws InvalidNameCharacterException, IOException {
		this.name = name;
		this.persistentPath = persistentPath;
		this.config = config;
		this.timer = timer;
		this.waiters = new DeadlineWaitQueue<Waiter>(timer);
		this.isFanout = name.contains("+");
		this.journal = new Journal(new File(persistentPath), name, this, journalSyncScheduler, config.getSyncJournal(), packer);
		this.createTime = System.currentTimeMillis();
//		this.queueCollection = queueCollection;
		if(config.getExpireToQueue() != null)
			expireQueue = queueCollection.queue(config.getExpireToQueue(), null);

	}
	
	public PersistentQueue(String name, String persistentPath, QueueConfig config, Timer timer) throws Exception{
		this(name, persistentPath, config, timer, null, null, null);
	}
	
	
	private SortedSet<Integer> openTransactionIds(){
		TreeSet<Integer> ots = new TreeSet<Integer>();
		synchronized(openTransactions){
			ots.addAll(openTransactions.descendingKeySet());
		}
		return ots.descendingSet();
	}
	public synchronized int openTransactionCount(){
		return openTransactions.size();
	}
	public synchronized long length(){
		return queueLength;
	}
	public synchronized long bytes(){
		return queueSize;
	}
	
	public synchronized long maxMemoryBytes(){
		return config.getMaxMemorySize().longValue();
	}
	public synchronized long journalSize(){
		return journal.size();
	}
	public synchronized long journalTotalSize(){
		return  journal.getArchivedSize() + journalSize();
	} 
	public synchronized long currentAge(){
		if(queueSize == 0)return 0l;
		return currentAge;
	}
	
	
	public synchronized Boolean isClosed(){
		return closed || paused;
	}
	
	public synchronized Boolean inReadBehind(){
		return journal.isReadBehind();
	}
	
	private final Long adjustExpiry(Long startingTime, Long expiry){
		if(config.getMaxAge() != null){
			long maxExpiry = startingTime + config.getMaxAge();
			if(expiry != null){
				if(expiry < maxExpiry)return expiry; 
				else return maxExpiry;
			}else{
				return maxExpiry;
			}
			
		}
		else return expiry;
	}
	
	/*
	 * 新算法：
	 * 1.当 queue为空并且journal.size > defaultJournalSize时，rewrite当前queue
	 * 2.当queue不为空且 journal.size >= maxMemorySize
	 * 2.1整个journalsize 大于 maxJournalSize 
	 * 2.1.1 当 这时在 fillreadbehind模式下，journal.rotate方法被调用 并且设置 checkpoint 保存现场
	 * 2.1.2当这时并不在fillreadbehind模式下，直接rewrite 内存到 新文件 
	 * 2.2整个journalSize 小于maxJournalSize 
	 * 直接rotate文件 
	 * 
	 */
	public final void checkRotateJournal() throws IOException, InterruptedException{
		
		if(log.isDebugEnabled()){
			log.debug("Check Rotate Journal method jounral.size = " + journal.size() + "; queueLength = "+ queueLength + "; config.defaultJournalSize = " + config.getDefaultJournalSize()
					+"; config.getMaxMemorySize = " + config.getMaxMemorySize()
					);
		}
	    if (queueLength == 0 && journal.size() >= config.getDefaultJournalSize().longValue()) {
	    	if(log.isDebugEnabled())
	    		log.debug("Rewriting journal file for '{}' (qsize={})", new Object[]{name, queueSize});
	        journal.rewrite(openTransactions.values(), queue);
	    } else if (journal.size() >= config.getMaxMemorySize().longValue()) {
	    	if(log.isDebugEnabled())
	    		log.debug("Rotating journal file for '{}' (qsize={})",new Object[]{name, queueSize});
	        if((journal.size() + journal.getArchivedSize()) >= config.getMaxJournalSize().longValue()){ 
	        	if(journal.isReadBehind()){
	        		//在fillreadbehind的模式下保存现场，并生成 checkpoint 留待 reader 读取到 checkpoint 位置时 进行 pack操作
	        		Set<QItem> reservedItems = new HashSet<QItem>();
	        		reservedItems.addAll(openTransactions.values());
	        		if(log.isDebugEnabled())
		        		log.debug("check Rotate Journal (journal.size() + journal.getArchivedSize()) >= config.getMaxJournalSize() and is in read behind rotate" );
	        	    journal.rotate(reservedItems, true);  
	        	}else{//否则 所有的有用数据都在内存中 可以直接 rewrite到 新文件中 
	        		  //删除所有旧文件  
	        		  //dump数据比较大 有可能会比较慢  ,留待后期优化
	        		if(log.isDebugEnabled())
		        		log.debug("check Rotate Journal (journal.size() + journal.getArchivedSize()) >= config.getMaxJournalSize() and  is not in read behind rewrite" );
	        		journal.rewrite(openTransactions.values(), queue);
	        	}
	        }else{
	        	if(log.isDebugEnabled())
	        		log.debug("check Rotate Journal (journal.size() + journal.getArchivedSize()) <= config.getMaxJournalSize()  rotate" );
	    	    journal.rotate(openTransactions.values(), false);
	        }
	    }
	}
	
	public Boolean isReadyForExpiration(){
	    if (config.getMaxQueueAge() != null && queue.isEmpty() && System.currentTimeMillis() > createTime + config.getMaxQueueAge()) {
	        return true;
	     } else {
	        return false;
	     }
	}
	
	public synchronized Boolean add(byte[] value, Long expiry, Integer xid, Long addTime) throws IOException, InterruptedException {
		if(closed || value.length > config.getMaxItemSize()){
			return false;
		}
		if(config.getFanoutOnly() && !isFanout){
			return true;
		}

		//fix me
		while(((config.getMaxItems() != null ) && queueLength > config.getMaxItems().longValue()) || (config.getMaxSize() != null && (queueSize >= config.getMaxSize().longValue()))){
			if(!config.getDiscardOldWhenFull())return false;
			_remove(false, null);
			totalDiscarded.getAndIncrement();
			if(logger.isDebugEnabled())
				logger.debug("remove before add ");
			if(config.getKeepJournal())journal.remove();
		}

		long now = System.currentTimeMillis();
		QItem item = new QItem(now, adjustExpiry(now, expiry), value, 0);
		if(logger.isDebugEnabled())
			logger.debug("add = " + item);
		if(config.getKeepJournal()){
			checkRotateJournal();
			if(!journal.isReadBehind() && (queueSize >= config.getMaxMemorySize().longValue())){
				  log.info("Dropping to read-behind for queue '{}' ({})", name, queueSize);
		          journal.startReadBehind();
			}
		}
		if(xid != null)openTransactions.remove(xid);
		_add(item);
		if(config.getKeepJournal()){
			if(xid == null){
				journal.add(item);
			}else{
				journal.continue0(xid, item);
			}
		}
		return true;
	}
	
	public Boolean add(byte[] value) throws IOException, InterruptedException{
		return add(value, null, null, System.currentTimeMillis());
	}
	
	public Boolean add(byte[] value, long expiry) throws IOException, InterruptedException{
		return add(value, expiry, null, System.currentTimeMillis());
	}
	public Boolean continue0(Integer xid, byte[] value) throws IOException, InterruptedException{
		return add(value, null, xid, System.currentTimeMillis());
	}
	public Boolean continue0(Integer xid, byte[] value, long expiry) throws IOException, InterruptedException{
		return add(value, expiry, xid, System.currentTimeMillis());
	}
	
	public QItem peek() throws IOException{
		synchronized(this){
			if(closed || paused || queueLength == 0){
				return null;
			}
			else 
				return _peek();
		}
	}
	
	public synchronized QItem remove(Boolean transaction) throws IOException, InterruptedException{
		if(closed || paused || queueLength == 0){
			return null;
		}
		else{
			if (transaction) totalTransactions.getAndIncrement();
			QItem item = _remove(transaction, null);
			if(config.getKeepJournal() && item != null){
				if(transaction) journal.removeTentative(item.getXid());
				else journal.remove();
				checkRotateJournal();
			}
			return item;
		}
		
	}
	
	public QItem remove() throws IOException, InterruptedException{
		return remove(false);
	}
	
//	public void operateReact(QItem op, long timeout, MethodPack pack){
//		
//	}
	
	public void unremove(Integer xid) throws IOException{
		synchronized(this){
			if(!closed){
				if(config.getKeepJournal()){
					journal.unremove(xid);
				}
				boolean flag = _unremove(xid);
				if(flag)totalCanceledTransactions.getAndIncrement();
			}
		}
	}
 
	public void confirmRemove(Integer xid) throws IOException {
		synchronized (this){
			if (!closed) {
				if (config.getKeepJournal()) journal.confirmRemove(xid);
				openTransactions.remove(xid);
		     }
		 }
	}
	
	private QItem _remove(boolean transaction, Integer xid) throws IOException{
	    discardExpired(false);
	    if (queue.isEmpty()) return null;
	    QItem item = queue.poll();
	    if(logger.isDebugEnabled())
	    	logger.debug("queue poll _remove = " + item);
	    long len = item.getData().length;
	    queueSize -= len;
	    memoryBytes -= len;
	    queueLength -= 1;
	    fillReadBehind();
	    currentAge = item.getAddTime();
	    if (transaction) {
	      item.setXid(xid == null? nextXid() : xid);
	      openTransactions.put(item.getXid(), item);
	    }
	   	return item;
	}
	private boolean _unremove(Integer xid){
		QItem item = openTransactions.remove(xid);
		if(item != null){
			queueLength += 1;
			queueSize += item.getData().length;
			queue.addFirst(item);
			memoryBytes += item.getData().length;
			return true;
		}
		return false;
	}
	private void _add(QItem qItem) throws IOException{
		discardExpired(false);
		if(!journal.isReadBehind()){
			queue.add(qItem);
			memoryBytes += qItem.getData().length;
		}
	    putItems.getAndIncrement();
	    putBytes.getAndAdd(qItem.getData().length);
	    queueSize += qItem.getData().length;
	    queueLength += 1;
	}
	private QItem _peek() throws IOException{
	    discardExpired(false);
	    if (queue.isEmpty()) return null; else  return queue.poll();
	}
	
	private final int discardExpired(Boolean limit) throws IOException, InterruptedException{
		List<QItem> toRemove = new ArrayList<QItem>();
		synchronized(this) {
	        boolean _continue = true;

	        Boolean hasLimit = limit && (config.getMaxExpireSweep() > 0);
	        while (_continue) {
	          if (queue.isEmpty() || (hasLimit && toRemove.size() >= config.getMaxExpireSweep()) || journal.isReplaying()) {
	            _continue = false;
	          } else {
	            Long realExpiry = adjustExpiry(queue.peek().getAddTime(), queue.peek().getExpiry());
	            if (realExpiry != null && realExpiry < System.currentTimeMillis()) {
	              totalExpired.incrementAndGet();
	              QItem item = queue.poll();
	              int len = item.getData().length;
	              queueSize -= len;
	              memoryBytes -= len;
	              queueLength -= 1;
	              fillReadBehind();
	              if (config.getKeepJournal()) journal.remove();
	              toRemove.add(item);
	            } else {
	              _continue = false;
	            }
	          }
	        }
	      }

		if(expireQueue != null)
			for(QItem item : toRemove){
				expireQueue.add(item.getData()); 
		     }
	     return toRemove.size();
	}
	
	/**
	 * @throws IOException 
	 * @throws Exception 
	 */
	private void fillReadBehind() throws IOException{
	    while (config.getKeepJournal() && journal.isReadBehind() && memoryBytes < config.getMaxMemorySize()) {
	      journal.fillReadBehind();
	      if (!journal.isReadBehind()) {
	        log.info("Coming out of read-behind for queue '{}'", name);
	      }
	    }
	
	}
	
	public void replayJournal() throws IOException {
	    if (!config.getKeepJournal()) return;
	    log.info("Replaying transaction journal for '{}'", name);
	    xidCounter = 0;
	    journal.replay();
	    log.info("Finished transaction journal for '{}' ({} items, {} bytes) xid={}", new Object[]{ name, queueLength, journal.size(), xidCounter});
	    journal.open();

	    // now, any unfinished transactions must be backed out.
	    for (Integer xid : openTransactionIds()) {
	    	if(logger.isDebugEnabled())
	    		logger.debug("openTransaction Ids = " + xid);
	    	journal.unremove(xid);
	    	_unremove(xid);
	    }
	}
	
	private Integer nextXid(){
	    do {
	        xidCounter += 1;
	      } while ((openTransactions.containsKey(xidCounter)) || (xidCounter == 0));
	     return xidCounter;
	}

	public Deque<QItem> getQueue() {
		return queue;
	}

	public void setQueue(Deque<QItem> queue) {
		this.queue = queue;
	}

	public Long getMemoryBytes() {
		return memoryBytes;
	}

	public void setMemoryBytes(Long memoryBytes) {
		this.memoryBytes = memoryBytes;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(Long queueSize) {
		this.queueSize = queueSize;
	}

	public Long getQueueLength() {
		return queueLength;
	}

	public void setQueueLength(Long queueLength) {
		this.queueLength = queueLength;
	}

	public TreeMap<Integer, QItem> getOpenTransactions() {
		return openTransactions;
	}

	public void setOpenTransactions(TreeMap<Integer, QItem> openTransactions) {
		this.openTransactions = openTransactions;
	}
	
	public void replayJournalInnerMethod(JournalItem journalItem) throws IOException{
		if(journalItem instanceof JournalItem.Add){
			QItem item = ((JournalItem.Add)journalItem).getItem();
			_add(item);
			if (!journal.isReadBehind() && queueSize >= config.getMaxMemorySize()) {
				  log.info("Dropping to read-behind for queue '{}' ({} bytes)", name, queueSize);
				  journal.startReadBehind();
			}
		}
		else if(journalItem instanceof JournalItem.Remove){
			_remove(false, null);
			
		}
		else if(journalItem instanceof JournalItem.RemoveTentative){
			Integer xid = ((JournalItem.RemoveTentative)journalItem).getXid();
			_remove(true, xid);
			xidCounter = xid;
		}
		else if(journalItem instanceof JournalItem.SavedXid){
			Integer xid = ((JournalItem.SavedXid)journalItem).getXid();
			xidCounter = xid;
		}
		else if(journalItem instanceof JournalItem.Unremove){
			Integer xid = ((JournalItem.Unremove)journalItem).getXid();
			_unremove(xid);
		}
		else if(journalItem instanceof JournalItem.ConfirmRemove){
			Integer xid = ((JournalItem.ConfirmRemove)journalItem).getXid();
			openTransactions.remove(xid);
		}
		else if(journalItem instanceof JournalItem.Continue){
			QItem item = ((JournalItem.Continue)journalItem).getItem();
			Integer xid = ((JournalItem.Continue)journalItem).getXid();
		    openTransactions.remove(xid);
		    _add(item);
		}else{
			log.error("Unexpected item in journal: {}", journalItem);
		}
	}
	
	public void addItem(QItem item){
		 getQueue().add(item);
	     setMemoryBytes(getMemoryBytes() + item.getData().length);
	}
	
	public void close() {
		synchronized(this) {
			closed = true;
		    if (config.getKeepJournal())
			try {
				journal.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		 }
	}

	public QueueConfig getConfig() {
		return config;
	}

	public void setConfig(QueueConfig config) {
		this.config = config;
	}

	
	public void setup() throws IOException {
		synchronized (this){
			queueSize = 0l;
		    replayJournal();
		}
	}
	
	
	public Promise<QItem> waitRemove(Long deadline, boolean transaction) throws InterruptedException, ExecutionException, IOException{
	    Promise<QItem> promise = new Promise<QItem>(){
			@Override
			public void onCancelation() {
				if(waiter != null)
					waiters.remove(waiter); 
			}
	    };
	    QItem item = remove(transaction);
	    if(deadline != null && deadline > System.currentTimeMillis()){
	        Date now = new Date();
		    Date timeoutTime = new Date(deadline);
		    waitOperation(item, now, timeoutTime, promise);
			while(!promise.isDone() && item == null){//do nothing
				try {
					Thread.sleep(1);
					if(deadline <= System.currentTimeMillis()){
						promise.setValue(null);
						break;
					}
				} catch (Exception e) {
				}
				item = remove(transaction);
		    }
			if(item != null && !promise.isDone()){
				promise.setValue(item);
			}
	    }else{
	    	promise.setValue(item);
	    }

	    if(promise.get() != null){
	    	//hit
	    }else{
	    	//miss
	    }  
		return promise;
	}
	
	
	public Promise<QItem> waitPeek(Long deadline) throws InterruptedException, ExecutionException, IOException{
	    long startTime = System.currentTimeMillis();
	    Promise<QItem> promise = new Promise<QItem>(){
			@Override
			public void onCancelation() {
				if(waiter != null)
					waiters.remove(waiter); 
			}
	    	
	    };
    	QItem item = peek();
	    if(deadline != null && deadline > System.currentTimeMillis()){
		    Date now = new Date();
		    Date timeoutTime = new Date(deadline);
		    waitOperation(item, now, timeoutTime, promise);
		    while(!promise.isDone() && item == null){
				try {
					Thread.sleep(1);
					if(deadline <= System.currentTimeMillis()){
						promise.setValue(null);
						break;
					}
				} catch (Exception e) {
				}
		    	item = peek();
		    }
		    if(item != null && !promise.isDone()){
		    	promise.setValue(item);
		    }
	    }else{
	    	promise.setValue(item);
	    }

	    if(promise.get() != null){
	    	//hit
	    }else{
	    	//miss
	    }
	    
		return promise;
	}
	
	
	
	public void waitOperation(final QItem op, final Date startTime, final Date deadline, final Promise<QItem> promise){
		if(op == null &&  !closed && !paused && deadline != null  && deadline.getTime() > System.currentTimeMillis()){
			synchronized(this){
		        final Waiter w = waiters.add(deadline.getTime(), this, promise);
		        promise.setWaiter(w);
			}
		}else{
			promise.setValue(op);
		}
		
	}
	
	public void flush() throws IOException, InterruptedException{
		    while (remove(false) != null) { }
		    totalFlushes.getAndIncrement();
	}
	
	
	public void destroyJournal(){
		synchronized(this){
			if(config.getKeepJournal())journal.erase();
		}
	}


	public Integer discardExpired(boolean limit) throws IOException{
		List<QItem> toRemove = new ArrayList<QItem>();
		synchronized(this){
			boolean _continue = true;
			boolean hasLimit = limit && config.getMaxExpireSweep() != null && config.getMaxExpireSweep() > 0;
			while(_continue){
				if(queue.isEmpty() || (hasLimit && config.getMaxExpireSweep() != null && toRemove.size() >= config.getMaxExpireSweep()) || journal.isReplaying()){
					_continue = false;
				}else{
					Long realExpiry = adjustExpiry(queue.peekFirst().getAddTime(), queue.peekFirst().getExpiry());
					if(realExpiry != null && realExpiry < System.currentTimeMillis()){
						totalExpired.getAndIncrement();
						QItem item = queue.getFirst();
						int len = item.getData().length;
						queueSize -= len;
						_memoryBytes -= len;
						queueLength -= 1;
					    fillReadBehind();
				        if (config.getKeepJournal()) journal.remove();
				        toRemove.add(item);
					}else{
						_continue = false;
					}
				}
			}
			for(QItem item : toRemove){
				try {
					if(expireQueue != null)
						expireQueue.add(item.getData());
				} catch (InterruptedException e) {
					logger.error("expireQueue add item error item = "+ item);
					e.printStackTrace();
				}
			}
		    return toRemove.size();
		
		}
	}
	
	public void evictWaiters(){
		synchronized(this){
		     waiters.evictAll(); 
		}
	}
	
	public DeadlineWaitQueue<Waiter> getWaiters(){
		return waiters;
	}
	
	
}
