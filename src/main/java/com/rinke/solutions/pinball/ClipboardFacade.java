package com.rinke.solutions.pinball;

public interface ClipboardFacade {

	public Object getContents(String transfer);
	public void setContents(Object[] contents, String[] transfers);
	public String[] getAvailableTypeNames();
	
}
