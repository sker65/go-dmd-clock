package com.rinke.solutions.pinball.view.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;


public abstract class AbstractModel {
	
	protected PropertyChangeSupport change = new PropertyChangeSupport(this);

	protected void firePropertyChange(String propName, Object oldValue, Object newValue) {
		change.firePropertyChange(propName, oldValue, newValue);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		change.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		change.removePropertyChangeListener(l);
	}

}
