package com.lenovo.ecs.eyas.journal;

public class JournalItemWrapper {
	JournalItem item;
	Integer length;
	JournalItemWrapper(JournalItem instance, Integer i) {
		this.item = instance;
		this.length = i;
	}
	
	public boolean equals(JournalItemWrapper wrapper){
		if(wrapper == null)return false;
		if(this.item.equals(wrapper.item) && this.length.intValue() == wrapper.length.intValue())
			return true;
		return false;
	}

	public JournalItem getItem() {
		return item;
	}

	public void setItem(JournalItem item) {
		this.item = item;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}
	
}
