package com.lenovo.ecs.eyas.journal;

import java.io.Serializable;

import com.lenovo.ecs.eyas.QItem;

public interface JournalItem extends Serializable{
	
	public static class Add implements JournalItem{
		private QItem item;
		public Add(QItem item){
			this.item = item;
		}
		public QItem getItem() {
			return item;
		}
		public void setItem(QItem item) {
			this.item = item;
		}
		
		
	}
	
	public static class Remove implements JournalItem{
		private Remove(){}
		public static final Remove instance = new Remove();
		public static Remove getInstance(){
			return instance;
		}
	}
	
	public static class RemoveTentative implements JournalItem{
		private Integer xid;
		public RemoveTentative(Integer xid){
			this.xid = xid;
		}
		public Integer getXid() {
			return xid;
		}
		public void setXid(Integer xid) {
			this.xid = xid;
		}
		
	}
	
	
	public static class SavedXid implements JournalItem{
		private Integer xid;
		public SavedXid(Integer xid){
			this.xid = xid;
		}
		public Integer getXid() {
			return xid;
		}
		public void setXid(Integer xid) {
			this.xid = xid;
		}
		
		
	}
	
	public static class Unremove implements JournalItem{
		private Integer xid;
		public Unremove(Integer xid){
			this.xid = xid;
		}
		public Integer getXid() {
			return xid;
		}
		public void setXid(Integer xid) {
			this.xid = xid;
		}
		
		
	}
	
	
	public static class ConfirmRemove implements JournalItem{
		private Integer xid;
		public ConfirmRemove (Integer xid){
			this.xid = xid;
		}
		public Integer getXid() {
			return xid;
		}
		public void setXid(Integer xid) {
			this.xid = xid;
		}
		
	}
	
	public static class Continue implements JournalItem{
		private QItem item;
		private Integer xid;
		public Continue (QItem item, Integer xid){
			this.item = item;
			this.xid = xid;
		}
		public QItem getItem() {
			return item;
		}
		public void setItem(QItem item) {
			this.item = item;
		}
		public Integer getXid() {
			return xid;
		}
		public void setXid(Integer xid) {
			this.xid = xid;
		}
		
		
	}
	
	public static class EndOfFile implements JournalItem{
		private EndOfFile(){}
		public final static EndOfFile instance = new EndOfFile();
		public static EndOfFile getInstance(){
			return instance;
		}
	}
	
	
	
}
