package com.lenovo.ecs.eyas.client.thrift;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import com.lenovo.ecs.eyas.client.ClientConfig;
import com.lenovo.ecs.eyas.thrift.Eyas;
import com.lenovo.ecs.eyas.thrift.Item;

public class EyasClient  implements Runnable{
	TTransport transport;
    Eyas.Client client; 
   
    
    public EyasClient(String host, int port) throws TTransportException{
    	transport = new TFramedTransport(new TSocket(host, port)); 
    	TProtocol protocol = new TCompactProtocol(transport); 
    	client = new Eyas.Client(protocol);
    	transport.open();
    }
	
	public List<Item> syncGet(String queue_name, int max_items, int timeout_msec, int auto_abort_msec) throws TException{
		List<Item> items = client.get(queue_name, max_items, timeout_msec, auto_abort_msec);
	    return items;
	}
	
	public int syncAdd(String queue_name, List<ByteBuffer> items, int expiration_msec) throws TException{
		return client.put(queue_name, items, expiration_msec);
	}
	
	public int syncConfirm(String queue_name, Set<Integer> ids) throws TException{
		return client.confirm(queue_name, ids);
	}
	
	public int syncAbort(String queue_name, Set<Integer> ids) throws TException{
		return client.abort(queue_name, ids);
	}
	
	public void close(){
		 transport.close();  
	}
	
	
	public void run(){
		for(int i=0;i<10000;i++){
			try {
				List<Item> items = syncGet("abc", 1, 1, 0);
			} catch (TException e) {
				e.printStackTrace();
			}
		}
		
	}
	public static void main(String[] args)throws Exception{
		
		EyasClient client = new EyasClient(ClientConfig.getConfig().getHost(), ClientConfig.getConfig().getPort());
		List<ByteBuffer> l = new ArrayList<ByteBuffer>();
		for(int i=0;i<10000000; i++){
			ByteBuffer item = ByteBuffer.wrap((i+"").getBytes());
			l.add(item);
		}
		
		client.syncAdd("abc", l, 0);
		
//		Thread[] ts = new Thread[10];
//		long start = System.currentTimeMillis();
//		for(int i=0; i<10; i++){
//			EyasClient client = new EyasClient(ClientConfig.getConfig().getHost(), ClientConfig.getConfig().getPort());
//			Thread t = new Thread(client);
//			ts[i] = t;
//			ts[i].start();
//		}
//	
//		for(int i=0; i<10; i++){
//			ts[i].join();
//		}
//		long end = System.currentTimeMillis();
//		System.out.println("time = " + (end -start));
	}

}
