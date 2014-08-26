package com.lenovo.ecs.eyas.journal;

import com.lenovo.ecs.eyas.QItem;

public class PackRequest{
  private Journal journal;
  private CheckPoint checkPoint;
  private Iterable<QItem> openItems;
  private int pendUpDeletes;
  private Iterable<QItem> queueState;
  public PackRequest(Journal journal, CheckPoint checkPoint, Iterable<QItem> openItems, int pendUpDeletes, Iterable<QItem> queueState){
	  this.journal = journal;
	  this.checkPoint = checkPoint;
	  this.openItems = openItems;
	  this.pendUpDeletes = pendUpDeletes;
	  this.queueState = queueState;
  }
	public Journal getJournal() {
		return journal;
	}
	public void setJournal(Journal journal) {
		this.journal = journal;
	}
	public CheckPoint getCheckPoint() {
		return checkPoint;
	}
	public void setCheckPoint(CheckPoint checkPoint) {
		this.checkPoint = checkPoint;
	}
	public Iterable<QItem> getOpenItems() {
		return openItems;
	}
	public void setOpenItems(Iterable<QItem> openItems) {
		this.openItems = openItems;
	}
	public int getPendUpDeletes() {
		return pendUpDeletes;
	}
	public void setPendUpDeletes(int pendUpDeletes) {
		this.pendUpDeletes = pendUpDeletes;
	}
	public Iterable<QItem> getQueueState() {
		return queueState;
	}
	public void setQueueState(Iterable<QItem> queueState) {
		this.queueState = queueState;
	}
}

