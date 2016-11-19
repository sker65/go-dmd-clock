package com.rinke.solutions.pinball.animation;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractModel {
	/**
	 * Should be transient to not conflict with serialization.
	 */
	private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	/**
	 * Adds a property change listener.
	 * 
	 * @param listener The property change listeter
	 */
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
		//log.debug("property change support listener added");
	}

	/**
	 * Removes a property change listener.
	 * 
	 * @param listener The property change listener
	 */
	public void removePropertyChangeListener(final PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	/**
	 * Convenience method to fire property change events.
	 * 
	 * @param propertyName Name of the property
	 * @param oldValue Old value of the property
	 * @param newValue New value of the property
	 */
	protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
		String pName = propertyName;
		if( propertyName.startsWith("this.")) pName = propertyName.substring(5);
		if (oldValue == null || newValue == null || !oldValue.equals(newValue))
			log.info("property changed '{}': {} -> {}", pName, oldValue, newValue);
		propertyChangeSupport.firePropertyChange(pName, oldValue, newValue);
	}
}
