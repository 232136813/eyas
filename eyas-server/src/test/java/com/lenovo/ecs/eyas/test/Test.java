package com.lenovo.ecs.eyas.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.Validate;

public class Test extends Thread{
	ServerSocket ssocket;
	final static Set<Socket> sockets = new HashSet<Socket>();
	public Test(){
		try {
			this.ssocket = new ServerSocket(11210, 3);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Test2 t2 = new Test2();
		t2.start();
	}
	
	public void run(){
		while (true) {
			try {
				Socket socket = this.ssocket.accept();
				sockets.add(socket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public static class Test2 extends Thread{
			public void run(){
				while(true){
					try {
						Thread.currentThread().sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					for(Socket s : sockets){
						try {
							System.out.println(s.getInputStream().read());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
	}
	
	public static void main(String[] args) throws IOException {
		Test t = new Test();
		t.start();
		for(int i=0;i<10;i++){
			Socket socket = new Socket("127.0.0.1",11210);
			socket.getOutputStream().write(i);
			socket.getOutputStream().flush();
//			System.out.println(socket);
		}
	
	

	}
}

