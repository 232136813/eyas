package com.lenovo.ecs.eyas.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig extends Properties{
	private static final ClientConfig config = new ClientConfig();
	private ClientConfig(){
		try {
			init();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private final void init() throws IOException{
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("client.properties");
		load(is);
	}
	public static ClientConfig getConfig(){
		return config;
	}
	
	public String getHost(){
		return config.getProperty("host");
	}
	
	public int getPort(){
		try{
			String portString = config.getProperty("port") ;
			if(portString != null)
				return Integer.parseInt(portString);
		}catch(Exception e){}
		return 0;
	}
}
