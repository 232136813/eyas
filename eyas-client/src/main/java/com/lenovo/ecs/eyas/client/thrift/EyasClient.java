package com.lenovo.ecs.eyas.client.thrift;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

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

public class EyasClient {
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
	
	public void close(){
		 transport.close();  
	}
	
	public static void main(String[] args)throws Exception{
		EyasClient client = new EyasClient(ClientConfig.getConfig().getHost(), ClientConfig.getConfig().getPort());
	
//		for(int j=0; j<100; j++){
//			List<ByteBuffer> l = new ArrayList<ByteBuffer>();
//			for(int i=0; i<5000;i++){
//				l.add(ByteBuffer.wrap((i+"").getBytes()));		
//			}
//			int i = client.syncAdd("abc", l, 0);
//			System.out.println(i);
//		}

		for(int i=0;i<100;i++){
			List<Item> items = client.syncGet("abc", 1, 1, 0);
			System.out.println(new String(items.get(0).getData()));
		}

	

	}

}
