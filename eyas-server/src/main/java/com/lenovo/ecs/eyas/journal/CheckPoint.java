package com.lenovo.ecs.eyas.journal;

import java.util.Collection;
import java.util.List;

import com.lenovo.ecs.eyas.QItem;

public class CheckPoint{
	private String fileName;
	private Collection<QItem> reservedItems;
	public CheckPoint(String fileName, Collection<QItem> items){
		this.fileName = fileName;
		this.reservedItems = items;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public Collection<QItem> getReservedItems() {
		return reservedItems;
	}
	public void setReservedItems(List<QItem> reservedItems) {
		this.reservedItems = reservedItems;
	}

  }