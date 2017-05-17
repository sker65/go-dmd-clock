package com.rinke.solutions.pinball.view.model;

import java.beans.PropertyChangeListener;

public interface PropertyChangeSupported {
	
	public void addPropertyChangeListener(PropertyChangeListener l);
	public void removePropertyChangeListener(PropertyChangeListener l);
	
}
