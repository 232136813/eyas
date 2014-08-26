package com.lenovo.ecs.eyas.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lenovo.ecs.eyas.QItem;
import com.lenovo.ecs.eyas.exception.BrokenItemException;
import com.lenovo.ecs.eyas.journal.Journal;
import com.lenovo.ecs.eyas.journal.JournalItem;
import com.lenovo.ecs.eyas.journal.JournalItemWrapper;
import com.lenovo.ecs.eyas.journal.JournalItem.Add;
import com.lenovo.ecs.eyas.journal.JournalItem.ConfirmRemove;
import com.lenovo.ecs.eyas.journal.JournalItem.Continue;
import com.lenovo.ecs.eyas.journal.JournalItem.Remove;
import com.lenovo.ecs.eyas.journal.JournalItem.RemoveTentative;
import com.lenovo.ecs.eyas.journal.JournalItem.SavedXid;
import com.lenovo.ecs.eyas.journal.JournalItem.Unremove;



/**
 * 手动 pack工具类
 * @author songkun1
 *
 */
public class JournalPackerUtil {
	private static final Logger logger = LoggerFactory.getLogger(JournalPackerUtil.class);
	List<String> fileNames;
	String newFileName;
	List<Journal> journals = new ArrayList<Journal>();
	Iterator<JournalItemWrapper> remover = null;
	Iterator<JournalItemWrapper> adder = null;
	FileChannel writer = null;
	List<QItem> adderStack = new ArrayList<QItem>();
	Map<Integer, QItem> openTransactions = new HashMap<Integer, QItem>();
	int currentXid = 0;
	long offset = 0L;
	long adderOffset = 0L;
	long lastUpdate = 0l;
	long lastAdderUpdate = 0l;

	
	public JournalPackerUtil(List<String> fileNames, String newFileName) throws BrokenItemException, IOException{
		this.fileNames = fileNames;
		this.newFileName = newFileName;
		for(String name : fileNames){
			journals.add(new Journal(name, null));
		}
		ArrayList<JournalItemWrapper> remover_ = new ArrayList<JournalItemWrapper>();
		ArrayList<JournalItemWrapper> adder_ = new ArrayList<JournalItemWrapper>();
		for(Journal j : journals){
//			remover_.addAll(j.walk());
//			adder_.addAll(j.walk());
		}
		remover = remover_.iterator();
		adder = adder_.iterator();
		writer = new FileOutputStream(newFileName, false).getChannel();
	}
	
	private QItem advanceAdder()  {
	    if (!adderStack.isEmpty()) {
	      return adderStack.remove(0);
	    } else {
	      if (!adder.hasNext()) {
	        return null;
	      } else {
	    	  	JournalItemWrapper wrapper = adder.next();
	    	  	adderOffset += wrapper.getLength();
	        if (adderOffset - lastAdderUpdate > 1024 * 1024) {
	        	f2(offset, adderOffset);
	          	lastAdderUpdate = adderOffset;
	        }
	        if(wrapper.getItem() instanceof JournalItem.Add){
	        	return ((JournalItem.Add)(wrapper.getItem() )).getItem();
	        }else{
	        	return advanceAdder();
	        }
	      }
	    }
	}
	
	
	public Journal apply() throws IOException{
		while(remover.hasNext()){
			JournalItemWrapper w = remover.next();
			if(w.getItem() instanceof JournalItem.Continue){
				openTransactions.remove(((JournalItem.Continue)(w.getItem())).getXid());
			}
			if(w.getItem() instanceof JournalItem.Remove){
				advanceAdder();
			}
			if(w.getItem() instanceof JournalItem.RemoveTentative){
				 do {
			        currentXid += 1;
			     } while (openTransactions.containsKey(currentXid));
				 QItem qitem = advanceAdder();
				 qitem.setXid(currentXid); 
			     openTransactions.put(currentXid, qitem);
			}
			
			if(w.getItem() instanceof JournalItem.SavedXid){
				currentXid = ((JournalItem.SavedXid)(w.getItem())).getXid();
			}
			
			if(w.getItem() instanceof JournalItem.Unremove){
				Integer  xid = ((JournalItem.Unremove)(w.getItem())).getXid();
				QItem qItem= openTransactions.remove(xid);
				adderStack.add(qItem);
			}
			
			if(w.getItem() instanceof JournalItem.ConfirmRemove){
				Integer  xid = ((JournalItem.ConfirmRemove)(w.getItem())).getXid();
				 openTransactions.remove(xid);
			}
		    offset += w.getLength();
		    if (offset - lastUpdate > 1024 * 1024) {
		    	  f2(offset, adderOffset);
		    	  lastUpdate = offset;
		    }
		}
		// now write the new journal.
  		f2(0, 0);
	    Journal out = new Journal(newFileName, null);
	    out.open();
	    List<QItem> remaining = new ArrayList<QItem>();
	    QItem item = null;
	    while((item = advanceAdder()) != null){
	    	remaining.add(item);
	    }
	    out.dump(openTransactions.values(), remaining);
	    out.close();
	    return out;
	}
	
	private void f2(long x, long y){}
}
