package com.lenovo.ecs.eyas.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.digester3.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EyasConfigLoader {
	private static final Logger logger = LoggerFactory.getLogger(EyasConfig.class);
	public static final String EYAS_CONFIG = "eyas.xml";
	private EyasConfigLoader(){
		loadEyasConfig();
	}
	public static final EyasConfigLoader instance = new EyasConfigLoader();
	private volatile EyasConfigBuilder builder;
	private volatile EyasConfig config;
	private volatile Map<String, QueueConfigBuilder> queueConfigsBuilders;
	private volatile Map<String, AliasConfigBuilder> aliasConfigsBuilders;
    private void loadEyasConfig() {
    	try{
    		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(EYAS_CONFIG);
	        Digester digester = new Digester();
	        digester.setValidating(false);
     
	        digester.addObjectCreate("eyas", EyasConfigBuilder.class);
	        digester.addSetProperties("eyas");
	  
	        digester.addObjectCreate("eyas/queue", QueueConfigBuilder.class);
	        digester.addSetProperties("eyas/queue");
	        digester.addSetNext("eyas/queue", "addQueueConfig");
	    
	        digester.addObjectCreate("eyas/alias", AliasConfigBuilder.class);
	        digester.addSetProperties("eyas/alias");
	        digester.addSetNext("eyas/alias", "addAliasConfig");

	        
	        builder = (EyasConfigBuilder)digester.parse(is);
	        if(builder != null){
	        	queueConfigsBuilders = builder.getQueueConfigsBuilders();
	        	aliasConfigsBuilders = builder.getAliasConfigsBuilders();
	        	config = builder.build();
	        	if(config == null)config = new EyasConfig();
	        } 
        }catch(Exception e){
        	e.printStackTrace();
        	logger.error("load EyasConfig error", e);
        }

    }
    
    public EyasConfigBuilder getEyasConfigBuilder(){
    	return builder;
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

	public static EyasConfigLoader getInstance(){
    	return instance;
    }
	
	public EyasConfig getEyasConfig(){
		return config;
	}
	
}
