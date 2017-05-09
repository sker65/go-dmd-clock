package com.rinke.solutions.pinball.view.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.databinding.conversion.IConverter;

public class TypedLabel implements IConverter {
	private static final long serialVersionUID = 1L;
	
	public String type; // used for icon in labels
	public String label;
	
	PropertyChangeSupport change = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener l) {
		change.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		change.removePropertyChangeListener(l);
	}

	public TypedLabel( String type, String label) {
		this.type = type;
		this.label = label;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		firePropertyChange("type", this.type, this.type = type);
	}
	
	private void firePropertyChange(String propertyName, String oldValue, String newValue) {
		change.firePropertyChange(propertyName, oldValue, newValue);
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getTypedLabel() {
		return type + " - "+ label;
	}
	
	public void setLabel(String label) {
		firePropertyChange("label", this.label, this.label = label);
	}

	public Pair<String,String> getIconAndText() {
		return Pair.of(type.toLowerCase(), label);
	}

	@Override
	public String toString() {
		return String.format("TypedLabel [type=%s, label=%s]", type, label);
	}

	@Override
	public Object getFromType() {
		return String.class;
	}

	@Override
	public Object getToType() {
		return TypedLabel.class;
	}

	public TypedLabel() {
		super();
	}

	@Override
	public Object convert(Object fromObject) {
		String fromString = (String) fromObject;
		int p = fromString.indexOf(" - ");
		if( p != -1) {
			return new TypedLabel(fromString.substring(0,p), fromString.substring(p+3));
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypedLabel other = (TypedLabel) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
