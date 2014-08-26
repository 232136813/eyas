package com.lenovo.ecs.eyas;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.jboss.netty.util.HashedWheelTimer;

import com.lenovo.ecs.eyas.config.AliasConfigBuilder;
import com.lenovo.ecs.eyas.config.EyasConfig;
import com.lenovo.ecs.eyas.config.EyasConfigLoader;
import com.lenovo.ecs.eyas.config.QueueConfig;
import com.lenovo.ecs.eyas.config.QueueConfigBuilder;
import com.lenovo.ecs.eyas.thrift.Eyas;
import com.lenovo.ecs.eyas.thrift.impl.EyasThriftHandler;
import com.lenovo.ecs.eyas.util.TimerAdapter;
import com.sun.jdmk.comm.HtmlAdaptorServer;




public class EyasServer implements EyasServerMBean, Runnable {

	private TNonblockingServerSocket serverTransport;
	private TServer server;
	private static final AtomicInteger sessions = new AtomicInteger(0);
	private QueueCollection queueCollection;
	private Timer timer;
	private ScheduledExecutorService journalSyncScheduler;
	private QueueConfig defaultQueueConfig;
	private List<QueueConfigBuilder> queueConfigBuilders;
	private List<AliasConfigBuilder> aliasConfigBuilders;
	private EyasConfig config;
    private HtmlAdaptorServer adapter = null;
	
	/**
	 * @throws Exception 
	 */
	
	public EyasServer() throws Exception{
		this.config = EyasConfigLoader.getInstance().getEyasConfig();
		List<QueueConfigBuilder> tmp = new ArrayList<QueueConfigBuilder>();
		tmp.addAll(EyasConfigLoader.getInstance().getQueueConfigsBuilders().values());
		this.queueConfigBuilders = tmp;
		List<AliasConfigBuilder> atmp = new ArrayList<AliasConfigBuilder>();
		atmp.addAll(EyasConfigLoader.getInstance().getAliasConfigsBuilders().values());
		this.aliasConfigBuilders = atmp;
		this.defaultQueueConfig = new QueueConfigBuilder().build();
		this.journalSyncScheduler = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
		HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(1024*10, TimeUnit.MICROSECONDS);
		this.timer = new TimerAdapter(hashedWheelTimer);
		this.queueCollection = new QueueCollection(config.getQueuePath(), this.timer, journalSyncScheduler, defaultQueueConfig, queueConfigBuilders, aliasConfigBuilders);
		this.queueCollection.loadQueues();
		serverTransport = new TNonblockingServerSocket(this.config.getThriftListenPort());
		this.adapter = new HtmlAdaptorServer();
	}
	
	
	
	public void stop(){
		if(server != null){
			try{
				server.stop();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(serverTransport != null){
			try{
				serverTransport.close();
				System.out.println("Thrift server stop ...");
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(queueCollection != null){
			try {
				this.queueCollection.shutDown();
				System.out.println("QueueColletion stop ...");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(journalSyncScheduler != null){
			try{
				journalSyncScheduler.shutdown();
				System.out.println("JournalSyncScheduler stop ...");
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		if(timer != null)
			timer.cancel();
		if(adapter != null)
			adapter.stop();
		System.out.println("Eyas Stop");
		
	}
	
	public static AtomicInteger getSessions() {
		return sessions;
	}

	public void run(){
        final Eyas.Processor processor = new Eyas.Processor(new EyasThriftHandler(this.queueCollection, this.timer, null, null));  
        TThreadedSelectorServer.Args arg = new TThreadedSelectorServer.Args(serverTransport);   
        arg.protocolFactory(new TCompactProtocol.Factory());  
        arg.transportFactory(new TFramedTransport.Factory());  
        arg.processorFactory(new TProcessorFactory(processor));  
        TServer server = new TThreadedSelectorServer(arg);
		server.serve();
	}

	public static void main(String[] args) throws Exception {
		EyasServer eyas = new EyasServer();
	    MBeanServer server = MBeanServerFactory.createMBeanServer();
	    ObjectName eyasName = new ObjectName("Eyas:name=EyasServer");
	    server.registerMBean(eyas, eyasName);
	    ObjectName adapterName = new ObjectName("EyasAgent:name=htmladapter,port=8082");
	    server.registerMBean(eyas.adapter, adapterName);
	    eyas.adapter.start();
	    System.out.println("jmx start.....");
		Thread t = new Thread(eyas);
		t.setDaemon(true);
		t.start();
		System.out.println("Eyas Server Start");
	}

}
