package com.lenovo.ecs.eyas;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class QItem implements Serializable{
	private Long addTime;
	private Long expiry;
	private byte[] data;
	private Integer xid;
	
	public QItem(Long addTime, Long expiry, byte[] data, Integer xid){
		this.xid = xid;
		this.addTime = addTime;
		this.expiry = expiry;
		this.data = data;
	}
	
	
	public Long getAddTime() {
		return addTime;
	}


	public void setAddTime(Long addTime) {
		this.addTime = addTime;
	}


	public Long getExpiry() {
		return expiry;
	}


	public void setExpiry(Long expiry) {
		this.expiry = expiry;
	}


	public byte[] getData() {
		return data;
	}


	public void setData(byte[] data) {
		this.data = data;
	}


	public Integer getXid() {
		return xid;
	}


	public void setXid(Integer xid) {
		this.xid = xid;
	}

	public ByteBuffer pack(Byte opCode){
		return pack(opCode, 0);
	}
	
	public ByteBuffer pack(Byte opCode, Integer xid){
	    ByteBuffer buffer = ByteBuffer.allocate(data.length + 21 + ((xid.intValue() == 0)? 0 : 4));
//	    buffer.order(ByteOrder.LITTLE_ENDIAN);
	    buffer.put(opCode);
	    if (xid != 0) buffer.putInt(xid);
	    buffer.putInt(data.length + 16);
	    buffer.putLong(addTime);
	    if (expiry != null) {
	    	buffer.putLong(expiry);
	    } else {
	    	buffer.putLong(0);
	    }
	    buffer.put(data);
	    buffer.flip();
	    return buffer;
	}

	
	public static QItem unpack(byte[] data)throws IOException{
	    int dataLength = data.length;
	    if (dataLength < 16) {
	      throw new IOException("Data unexpectedly short (< 16 bytes); length = " + dataLength);
	    }
		ByteBuffer buffer = ByteBuffer.wrap(data);
//		buffer.order(ByteOrder.LITTLE_ENDIAN);
		byte[] bytes = new byte[data.length - 16];
		Long addTime = buffer.getLong();
		Long expiry = buffer.getLong();
		buffer.get(bytes);
		return new QItem(addTime, (expiry == 0)? null : expiry, bytes, 0);
	}
	
	public static QItem unpackOldAdd(byte[] data)throws IOException{
	    int dataLength = data.length;
	    if (dataLength < 4) {
	      throw new IOException("Data unexpectedly short (< 4 bytes); length = " + dataLength);
	    }
		ByteBuffer buffer = ByteBuffer.wrap(data);
		byte[] bytes = new byte[data.length - 4];
//		buffer.order(ByteOrder.LITTLE_ENDIAN);
		Integer expiry = buffer.getInt();
		buffer.get(bytes);
		return new QItem(System.currentTimeMillis(), (expiry == 0)? null : expiry*1000L, bytes, 0);
	}


	@Override
	public String toString() {
		return "QItem [addTime=" + addTime + ", expiry=" + expiry + ", data="
				+ Arrays.toString(data) + ", xid=" + xid + "]";
	}


	
}
