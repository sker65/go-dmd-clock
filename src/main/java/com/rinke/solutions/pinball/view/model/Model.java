package com.rinke.solutions.pinball.view.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.ObservableList;
import com.rinke.solutions.pinball.util.ObservableMap;

@Bean
public class Model {
	
	private PropertyChangeSupport change = new PropertyChangeSupport(this);

	private void firePropertyChange(String propName, Object oldValue, Object newValue) {
		change.firePropertyChange(propName, oldValue, newValue);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		change.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		change.removePropertyChangeListener(l);
	}
	
	public ObservableMap<String, CompiledAnimation> scenes = new ObservableMap<String, CompiledAnimation>(new LinkedHashMap<>());
	public ObservableMap<String, Animation> recordings = new ObservableMap<String, Animation>(new LinkedHashMap<>());
	
	public List<PalMapping> palMappings = new ArrayList<>();
	
	public Map<String,Integer> recordingsPosMap = new HashMap<String, Integer>();
	public Map<String,Integer> scenesPosMap = new HashMap<String, Integer>();
	
	public List<Mask> masks = new ArrayList<>();
	
	public Map<String,Set<Bookmark>> bookmarksMap = new HashMap<>();
	
	public ObservableList<Palette> palettes = new ObservableList<Palette>(new ArrayList<>());
	
	public boolean dirty;

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		firePropertyChange("dirty", this.dirty, this.dirty = dirty);
	}
	
	private Optional<PalMapping> search( TypedLabel sel) {
		for(PalMapping p : palMappings) {
			if( p.name.equals(sel.label)) return Optional.of(p);
		}
		return Optional.empty();
	}

	public Optional<PalMapping> getPalMapping(TypedLabel sel) {
		return sel==null?Optional.empty():search(sel);
	}


	public Optional<CompiledAnimation> getScene(TypedLabel sel) {
		return sel==null?Optional.empty():Optional.ofNullable(scenes.get(sel.label));
	}
	
	public Optional<Animation> getRecording(TypedLabel sel) {
		return sel==null?Optional.empty():Optional.ofNullable(recordings.get(sel.label));
	}
}
