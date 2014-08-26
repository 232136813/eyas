package com.lenovo.ecs.eyas.journal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lenovo.ecs.eyas.PersistentQueue;
import com.lenovo.ecs.eyas.QItem;
import com.lenovo.ecs.eyas.exception.BrokenItemException;
import com.lenovo.ecs.eyas.journal.PeriodicSyncFile.TimestampedPromise;
import com.lenovo.ecs.eyas.util.StorageUnit;

public class Journal {
	private static final Logger logger = LoggerFactory.getLogger(Journal.class);

	private File queuePath;
	private String queueName;
	private PersistentQueue queue;
	private Long syncJournal;
	private ScheduledExecutorService syncScheduler;
	private AtomicInteger outstandingPackRequests = new AtomicInteger(0);
	private File queueFile = null;
	
	private PeriodicSyncFile writer = null;
	private FileChannel reader = null;
	private String readerFileName = null;
	private FileChannel replayer = null;
	private String replayerFileName = null;
	
	volatile private boolean closed = false;
	volatile private CheckPoint checkPoint;
	private long size = 0L;
	volatile private long archivedSize = 0L;
	private int removesSinceReadBehind = 0;
	private byte[] buffer = new byte[16];
	
	private ByteBuffer byteBuffer = null;
	
	private JournalPackerTask packer = null;
	
	private static final byte CMD_ADD = 0;
	private static final byte CMD_REMOVE = 1;
	private static final byte CMD_ADDX = 2;
	private static final byte CMD_REMOVE_TENTATIVE = 3;
	private static final byte CMD_SAVE_XID = 4;
	private static final byte CMD_UNREMOVE = 5;
	private static final byte CMD_CONFIRM_REMOVE = 6;
	private static final byte CMD_ADD_XID = 7;
	private static final byte CMD_CONTINUE = 8;
	private static final byte CMD_REMOVE_TENTATIVE_XID = 9;
	

	
	
	public Journal(File queuePath, String queueName, PersistentQueue queue, ScheduledExecutorService syncScheduler, Long syncJournal, JournalPackerTask packerTask) throws FileNotFoundException {
		this.queuePath = queuePath;
		this.queueName = queueName;
		this.queue = queue;
		this.syncScheduler = syncScheduler;
		this.syncJournal = syncJournal;		
		queueFile = new File(queuePath, queueName);
		checkIfQueuePathExists();
		byteBuffer = ByteBuffer.wrap(buffer);
//		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		packer = packerTask;
	}
	
	public Journal(String fullPath, PersistentQueue queue, Long syncJournal) throws FileNotFoundException{
		this(new File(fullPath).getParentFile(), new File(fullPath).getName(), queue, null, syncJournal, null);
	}
	
	public Journal(String fullPath, PersistentQueue queue) throws FileNotFoundException{
		this(fullPath, queue, null);
	}
	
	public void open()throws FileNotFoundException{
		open(queueFile);
	}
	public void open(File file) throws FileNotFoundException{
		writer = new PeriodicSyncFile(file, syncScheduler, syncJournal);
	}
	
	public void calculateArchiveSize() {
	    List<String> files = Journal.archivedFilesForQueue(queuePath, queueName);
	    long size = 0L;
	    for(String file : files){
	    	File f = new File(queuePath, file);
	    	if(f != null)
	    		size += f.length();
	    }
	    archivedSize = size;
	}
	
	private File uniqueFile(String infix, String suffix) throws IOException, InterruptedException{
	    File file = new File(queuePath, queueName + infix + System.currentTimeMillis() + suffix);
	    while (!file.createNewFile()) {
	      Thread.sleep(1);
	      file = new File(queuePath, queueName + infix + System.currentTimeMillis() + suffix);
	    }
	    return file;
	}
	
	public CheckPoint rotate(Collection<QItem> reservedItems, Boolean setCheckpoint) throws IOException, InterruptedException {
		writer.close();
	    File current = new File(queuePath, queueName);
	    File rotatedFile = uniqueFile(".", "");
	    /*note:
	     * <p> Many aspects of the behavior of this method are inherently
	     * platform-dependent: The rename operation might not be able to move a
	     * file from one filesystem to another, it might not be atomic, and it
	     * might not succeed if a file with the destination abstract pathname
	     * already exists.  The return value should always be checked to make sure
	     * that the rename operation was successful.</p>
	     * 
	     * win7 os can not be successful when the destination file already exists
	     * 
	     */
	    current.renameTo(rotatedFile);
	    size = 0;
	    calculateArchiveSize();
	    open();

	    if (readerFileName != null &&  readerFileName.equals(queueName)) {
	    	readerFileName = rotatedFile.getName();
	    }

	    if (setCheckpoint && checkPoint == null) {
	      checkPoint = new CheckPoint(rotatedFile.getName(), reservedItems);
	    }
	    return checkPoint;
	}
	

	public void rewrite(Iterable<QItem> reservedItems, Iterable<QItem> queue) throws IOException, InterruptedException {
	    writer.close();
	    File tempFile = uniqueFile("~~", "");
	    open(tempFile);
	    dump(reservedItems, queue);
	    writer.close();
	    logger.info("rewrite  file = " + reservedItems);
	    File packFile = uniqueFile(".", ".pack");
	    tempFile.renameTo(packFile);
	    // cleanup the .pack file:
	    List<String> files = Journal.archivedFilesForQueue(queuePath, queueName);
	    new File(queuePath, files.get(0)).renameTo(queueFile);
	    calculateArchiveSize();
	    open();
	}
	
	private void dump(Iterable<QItem> reservedItems, Iterable<QItem> openItems, int pentUpDeletes, Iterable<QItem> queue) throws IOException{
	    size = 0L;
	    if(reservedItems != null)
		    for (QItem item : reservedItems) {
		      add(item);
		      removeTentative(item.getXid());
		    }
	    if(openItems != null)
		    for (QItem item : openItems) {
		      add(item);
		    }
	    byte[] empty = new byte[0];
	    for (int i=0; i<pentUpDeletes; i++) {
	      add(false, new QItem(System.currentTimeMillis(), null, empty, 0));
	    }
	    if(queue != null)
		    for (QItem item : queue) {
		      add(false, item);
		    }
	}
	
	public void dump(Iterable<QItem> reservedItems, Iterable<QItem> queue) throws IOException{
		 dump(reservedItems, null, 0, queue);
	}
	
	public void close() throws IOException{
		writer.close();
		if(reader != null){
			reader.close();
			reader = null;
		}
		readerFileName = null;
	    closed = true;
	    waitForPacksToFinish();
	    
	    //shutdown 
	    //to be removed
	    syncScheduler.shutdown();
		
	}
	public void erase(){
		try{
			close();	
			List<String> files = Journal.archivedFilesForQueue(queuePath, queueName);
			for(String fn : files){
				File f = new File(fn);
				f.delete();
			}
			queueFile.delete();
		}catch(Exception e){
			
		}
	}
	public Boolean isReadBehind(){
		return reader != null;
	}
	
	public Boolean isReplaying(){
		return replayer != null;
	}
	public void add(Boolean allowSync, QItem item) throws IOException{
		ByteBuffer blob = item.pack(CMD_ADDX);
		size += blob.limit();
		writer.write(blob);
	}
	
	public void add(QItem item)throws IOException{
		add(true, item);
	}
	
	public void continue0(Integer xid, QItem item){
		removesSinceReadBehind += 1;
		ByteBuffer blob = item.pack(CMD_CONTINUE, xid);
		size += blob.limit();
		writer.write(blob);
		
	}
	
	public void remove() throws IOException{
		write(CMD_REMOVE);
		if (isReadBehind()) removesSinceReadBehind += 1;
	}
	
	public void removeTentative(Integer xid) throws IOException{
		write(CMD_REMOVE_TENTATIVE, xid);
	}
	
	public void unremove(Integer xid) throws IOException{
		if(logger.isDebugEnabled())
			logger.debug("unremove = " + xid);
		write(CMD_UNREMOVE, xid);
	}
	
	public void confirmRemove(Integer xid) throws IOException{
		write(CMD_CONFIRM_REMOVE, xid);
		if (isReadBehind()) removesSinceReadBehind += 1;
	}
	
	public void  startReadBehind() throws IOException{
		long pos = (replayer != null? replayer.position() : writer.position());
	    String fileName = (replayerFileName != null? replayerFileName : queueName);
		FileChannel rj = new FileInputStream(new File(queuePath, fileName)).getChannel();
		rj.position(pos);
		reader = rj;
		readerFileName = fileName;
		removesSinceReadBehind = 0;
		logger.info("Read-behind on '{}' starting at file {}", queueName, readerFileName);
	}
	
	public void fillReadBehind() throws IOException{
		long pos = replayer != null ? replayer.position() : writer.position();
		String fileName = (replayerFileName != null)? replayerFileName : queueName;
		if(reader != null){
			if(reader.position() == pos && readerFileName.equals(fileName)){
				reader.close();
				reader = null;
				readerFileName = null;
			}else{
				JournalItemWrapper wrapper = readJournalEntry(reader);
				if(wrapper.getItem() instanceof JournalItem.Add){
			       QItem item = ((JournalItem.Add)(wrapper.getItem())).getItem();
			       queue.addItem(item);
				}
				else if(wrapper.getItem() instanceof JournalItem.Remove){
					removesSinceReadBehind -= 1;
				}
				else if(wrapper.getItem() instanceof JournalItem.ConfirmRemove){
					removesSinceReadBehind -= 1;
				}
				else if(wrapper.getItem() instanceof JournalItem.Continue){
					removesSinceReadBehind -= 1;
					QItem item = ((JournalItem.Continue)(wrapper.getItem())).getItem();
					queue.addItem(item);
				}
				else if(wrapper.getItem() instanceof JournalItem.EndOfFile){
					String oldFileName = readerFileName;
					reader.close();
					readerFileName = Journal.journalsAfter(queuePath, queueName, readerFileName);
					reader = new FileInputStream(new File(queuePath, readerFileName)).getChannel();
					logger.info("Read-behind on '{}' moving from file {} to {}", new String[]{queueName, oldFileName, readerFileName});
					if(checkPoint != null && checkPoint.getFileName().equals(oldFileName)){
						submitPackRequest(checkPoint);
					}
					fillReadBehind();
				}
			}
		}
	}
	
	private void submitPackRequest(CheckPoint checkPoint){
        logger.info("Rewriting journal file from checkpoint for '{}' (qsize={})", queue.getName(), queue.getQueueSize());
        List<QItem> openItems = new ArrayList<QItem>();
        List<QItem> queueItems = new ArrayList<QItem>();
        openItems.addAll(queue.getOpenTransactions().values());
        queueItems.addAll(queue.getQueue());
        
		Set<Integer> knownXids = new HashSet<Integer>();
		for(QItem item : checkPoint.getReservedItems()){
			knownXids.add(item.getXid());
		}
		Set<Integer> currentXids = new HashSet<Integer>();
		Set<QItem> newlyOpenItems = new HashSet<QItem>();
		for(QItem item : openItems){
			currentXids.add(item.getXid());
			if(!knownXids.contains(item.getXid())){
				newlyOpenItems.add(item);
			}
		}
		Set<QItem> newlyClosedItems = new HashSet<QItem>();
		for(QItem item : checkPoint.getReservedItems()){
			if(!currentXids.contains(item.getXid())){
				newlyClosedItems.add(item);
			}
		}
		int negs = removesSinceReadBehind - newlyClosedItems.size();
		outstandingPackRequests.incrementAndGet();
		packer.add(new PackRequest(this, checkPoint, newlyOpenItems, negs, queueItems));
	}

	
	public void replay() throws IOException{
		String[] fileNames = queuePath.list();
		for(String fn : fileNames){
			if(fn.startsWith(queueName + "~~")){
				new File(queuePath, fn).delete();
			}
		}
		List<String> journals = Journal.journalsForQueue(queuePath, queueName);
		for(String  fileName : journals){
			logger.info("journales fileName = " + fileName);
			replayFile(queueName, fileName);
		}
	
	}
	
	public void replayFile(String name, String fileName){
		size = 0L;
		long lastUpdate = 0L;
		try{
			FileChannel in = new FileInputStream(new File(queuePath, fileName)).getChannel();
			try{
				replayer = in;
				replayerFileName = fileName;
				boolean done = false;
				do{
					JournalItemWrapper wrapper = readJournalEntry(in);
					if(logger.isDebugEnabled())
					logger.debug("readJournalEntry = " + wrapper.item);
					if(wrapper.getItem() instanceof JournalItem.EndOfFile){
						done = true;
					}
					else {
						size += wrapper.getLength();
						queue.replayJournalInnerMethod(wrapper.getItem());
						if(size  > lastUpdate + StorageUnit.MEGABYTE.toByte(10)){
							lastUpdate = size;
							logger.info("Continuing to read "+name+" journal ("+fileName+"); "+lastUpdate*10+" MB so far...");
						}	
					}
				}while(!done);
			}catch(BrokenItemException e){
		        logger.error("Exception replaying journal for "+name+": "+fileName,e);
		        logger.error("DATA MAY HAVE BEEN LOST! Truncated entry will be deleted.");
		        try {
					truncateJournal(e.lastValidPosition);
				} catch (FileNotFoundException e1) {
					throw e1;
				}
			}
		}catch(FileNotFoundException e){
	        logger.info("No transaction journal for '{}'; starting with empty queue.", name);
		}catch(IOException e){
	        logger.error("Exception replaying journal for '"+name+"': "+fileName, e);
	        logger.error("DATA MAY HAVE BEEN LOST!");
		}
	    replayer = null;
	    replayerFileName = null;
	}
	
	private void truncateJournal(Long position)throws FileNotFoundException, IOException{
		 FileChannel trancateWriter = new FileOutputStream(queueFile, true).getChannel();
	    try {  
	      trancateWriter.truncate(position);
	    } finally {
	      trancateWriter.close();
	    }
	}
	
	
	public JournalItemWrapper readJournalEntry(FileChannel in)throws BrokenItemException, IOException{
		byteBuffer.rewind();
	  	byteBuffer.limit(1);
	  	long lastPosition = in.position();
	  	int x = 0;
	    do {
	      x = in.read(byteBuffer);
	    } while (byteBuffer.position() < byteBuffer.limit() && x >= 0);

	    if (x < 0) {
	    	return new JournalItemWrapper(JournalItem.EndOfFile.getInstance(), 0);
	    } else {
	      try {
	        byte tmp = buffer[0];
	        byte[] data = null;
	        int xid = 0;
	        QItem item = null;
	        switch(tmp){
	          case CMD_ADD :
	        	data = readBlock(in);
	            return new JournalItemWrapper(new JournalItem.Add(QItem.unpackOldAdd(data)), 5 + data.length);
	          case CMD_REMOVE :
	            return new JournalItemWrapper(JournalItem.Remove.getInstance(), 1);
	          case CMD_ADDX :
	        	data = readBlock(in);
	            return new JournalItemWrapper(new JournalItem.Add(QItem.unpack(data)), 5 + data.length);
	          case CMD_REMOVE_TENTATIVE :
	        	xid = readInt(in);//?? fix bug ??
	            return new JournalItemWrapper(new JournalItem.RemoveTentative(xid), 5);
	          case CMD_SAVE_XID :
	            xid = readInt(in);
	            return new JournalItemWrapper(new JournalItem.SavedXid(xid), 5);
	          case CMD_UNREMOVE :
	            xid = readInt(in);
	            return new JournalItemWrapper(new JournalItem.Unremove(xid), 5);
	          case CMD_CONFIRM_REMOVE :
	        	xid = readInt(in);
	            return new JournalItemWrapper(new JournalItem.ConfirmRemove(xid), 5);
	          case CMD_ADD_XID :
	            xid = readInt(in);
	            data = readBlock(in);
	            item = QItem.unpack(data);
	            item.setXid(xid);
	            return new JournalItemWrapper(new JournalItem.Add(item), 9 + data.length);
	          case CMD_CONTINUE :
	             xid = readInt(in);
	             data = readBlock(in);
	             item = QItem.unpack(data);
	            return new JournalItemWrapper(new JournalItem.Continue(item, xid), 9 + data.length);
	          case CMD_REMOVE_TENTATIVE_XID :
	             xid = readInt(in);
	             return new JournalItemWrapper(new JournalItem.RemoveTentative(xid),5);
	          default  :
	            throw new BrokenItemException(lastPosition, new IOException("invalid opcode in journal: " + tmp + " at position " + (in.position() - 1)));
	        }

	      } catch (IOException e){
	    	  e.printStackTrace();
	    	  logger.error("BrokenItemException  lastPosition = " + lastPosition, e);
	          throw new BrokenItemException(lastPosition, e);
	      }
	    }
	}
	
	
	private byte[] readBlock(FileChannel in) throws IOException{
	    int size = readInt(in);
	    byte[] data = new byte[size];
	    ByteBuffer dataBuffer = ByteBuffer.wrap(data);
	    int x= 0;
	    do {
	      x = in.read(dataBuffer);
	    } while (dataBuffer.position() < dataBuffer.limit() && x >= 0);
	    if (x < 0) {
	      // we never expect EOF when reading a block.
	      throw new IOException("Unexpected EOF");
	    }
	    return data;
	}
	
	private int readInt(FileChannel in) throws IOException{
	    byteBuffer.rewind();
	    byteBuffer.limit(4);
	    int x = 0;
	    do {
	      x = in.read(byteBuffer);
	    } while (byteBuffer.position() < byteBuffer.limit() && x >= 0);
	    if (x < 0) {
	      // we never expect EOF when reading an int.
	      throw new IOException("Unexpected EOF");
	    }
	    byteBuffer.rewind();
	    return byteBuffer.getInt();
	}

	public TimestampedPromise write(Object ... items) throws IOException{
	    byteBuffer.clear();
	    for (Object item : items){
	    	if(item instanceof Byte){
	    		byteBuffer.put((Byte)item);
	    	}else if(item instanceof Integer){
	    		byteBuffer.putInt((Integer)item);
	    	}
	    }
	    byteBuffer.flip();
	    TimestampedPromise p = writer.write(byteBuffer);
	    size += byteBuffer.limit();
	    return p;
	}
	
	
	public void pack(PackRequest state) throws BrokenItemException, IOException, InterruptedException {
	    List<String> filenames = Journal.journalsBefore(queuePath, queueName, state.getCheckPoint().getFileName());
	    if(filenames != null) {
	    	 filenames.add(state.getCheckPoint().getFileName());
	    	 logger.info("Packing journals for '{}' ...", queueName);
	    	 File tempFile = uniqueFile("~~", "");
	    	 Journal journal = new Journal(tempFile.getAbsolutePath(), queue);
	    	 journal.open();
	    	 journal.dump(state.getCheckPoint().getReservedItems(), state.getOpenItems(), state.getPendUpDeletes(), state.getQueueState());
	    	 journal.close();
	    	 logger.info("Packing '{}' -- erasing old files.", queueName);
	    	 File packFile = new File(queuePath, state.getCheckPoint().getFileName()+ ".pack");
	    	 tempFile.renameTo(packFile);
	    	 calculateArchiveSize();
	    	 logger.info("Packing '{}' done: {}", queueName, Journal.journalsForQueue(queuePath, queueName).toString());
	    	 checkPoint = null;
	    }   	  
	}
	
	
	private void checkIfQueuePathExists() throws FileNotFoundException{
		if (!queuePath.exists()) throw new FileNotFoundException(queuePath + " does not exist.");  
	}
	
	public static List<String> journalsForQueue(File path, String queueName){
		List<String> fns = archivedFilesForQueue(path, queueName);
		if(fns != null)fns.add(queueName);
		return fns;
	}
	
	public static List<String> journalsBefore(File path, String queueName, String fileName){
		 List<String> journals = journalsForQueue(path, queueName);
		 int index = journals.indexOf(fileName);
		 return journals.subList(0, index);
	}
	
	
	public static String journalsAfter(File path, String queueName, String fileName){
		 List<String> journals = journalsForQueue(path, queueName);
		 if(journals == null)return null;
		 int index = journals.indexOf(fileName);
		 return journals.subList(index, journals.size()).get(0);
	}
	
	public static Set<String> getQueueNamesFromFolder(File path){
		String[] fs = path.list();
		Set<String> fileNames = new HashSet<String>();
		for(String fn : fs){
			if(!fn.contains("~~")){
				fileNames.add(fn.split("\\.")[0]);
			}
		}
		return fileNames;
	}
	
	
	private static Boolean cleanUpPackedFiles(File path,  TreeMap<Long, String> files){  
	    String packFileName = null;
	    Long packTimestamp = null;
	    for(Entry<Long, String> e : files.entrySet()){
	    	if(e.getValue() != null && e.getValue().endsWith(".pack")){
	    		packFileName = e.getValue();
	    		packTimestamp = e.getKey();
	    		break;
	    	}
	    }
	    if(packFileName != null){
	    	SortedMap<Long, String> doomed = files.headMap(packTimestamp, true);
	    	for(Entry<Long, String> e: doomed.entrySet()){
	    		if(!e.getValue().contains(".pack")){
		    		File f = new File(path, e.getValue());
		    		f.delete();
	    		}
	    	}
	    	String newFileName = packFileName.substring(0, packFileName.length() - 5);
	    	new File(path, packFileName).renameTo(new File(path, newFileName));
	    	return true;
	    }else
	    	return false;
	}
	
	public static List<String> archivedFilesForQueue(File path, String queueName){
		String[] totalFiles = path.list();	
	    if (totalFiles == null) {
	    	return null;
	    } else {
	    	TreeMap<Long, String> timedFiles = new TreeMap<Long, String>();
	    	for(String name : totalFiles){
	    		if(name.startsWith(queueName + ".")){
	    			String[] subNames = name.split("\\.");
	    			if(subNames.length >= 2){
	    				timedFiles.put(Long.parseLong(subNames[1]), name);
	    			}
	    		}
	    	}
	    	
	    	if(cleanUpPackedFiles(path, timedFiles)){
	    		return archivedFilesForQueue(path, queueName);
	    	}else{
	    		return Collections.list(Collections.enumeration(timedFiles.values()));
	    	}
	    }
	}

	  public void waitForPacksToFinish() {
	    while (outstandingPackRequests.get() > 0) {
		    try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    }
	  }
	  
   public long size(){
	   return size;
   }
	  
	  
	public long getArchivedSize() {
		return archivedSize;
	}

	public void setArchivedSize(long archivedSize) {
		this.archivedSize = archivedSize;
	}
	
	public AtomicInteger getOutstandingPackRequests() {
		return outstandingPackRequests;
	}

	public void setOutstandingPackRequests(AtomicInteger outstandingPackRequests) {
		this.outstandingPackRequests = outstandingPackRequests;
	}

	public static void main(String[] args) {
		  TreeMap map = new TreeMap();
		  map.put(3, "a");
		  map.put(1, "b");
		  map.put(2, "c");
		  map.put(5, "d");
		  map.put(4, "e");
		  System.out.println(map.firstKey());
		  
	}
	
	

}
