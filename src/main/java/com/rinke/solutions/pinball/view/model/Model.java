package com.rinke.solutions.pinball.view.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.ObservableList;
import com.rinke.solutions.pinball.util.ObservableMap;

@Bean
public class Model {
	
	@Value(key=Config.DMDSIZE)
	int dmdSizeDefault;
	
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
	
	public ObservableList<PalMapping> palMappings = new ObservableList<PalMapping>(new ArrayList<>());
	
	public Map<String,Integer> recordingsPosMap = new HashMap<String, Integer>();
	public Map<String,Integer> scenesPosMap = new HashMap<String, Integer>();
		
	public List<Mask> masks = new ArrayList<>();
	
	public Map<String,Set<Bookmark>> bookmarksMap = new HashMap<>();
	
	public ObservableList<Palette> palettes = new ObservableList<Palette>(new ArrayList<>());
	
	public boolean dirty;
	public String name;
	public String filename;
	public DmdSize dmdSize;

	private int numberOfMasks = 10;
	public List<String> inputFiles = new ArrayList<>();

	public void reset() {
		// dmd size to default
		dmdSize = DmdSize.fromOrdinal(dmdSizeDefault);
		palMappings.clear();
		scenes.clear();
		recordings.clear();
		dirty = false;
		name = null;
		filename = null;
		recordingsPosMap.clear();
		scenesPosMap.clear();
		masks.clear();
		masks.addAll(getDefaultMasks());
		palettes.clear();
		palettes.addAll(Palette.getDefaultPalettes());	
		inputFiles.clear();
	}

	private Collection<Mask> getDefaultMasks() {
		List<Mask> r = new ArrayList<>();
		byte[] data = new byte[dmdSize.planeSize];
		Arrays.fill(data, (byte)0xFF);
		for(int i=0; i< numberOfMasks ; i++) {
			r.add( new Mask(data, false));
		}
		return r;
	}

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

	public DmdSize getDmdSize() {
		return dmdSize;
	}

	public void setDmdSize(DmdSize dmdSize) {
		firePropertyChange("dmdSize", this.dmdSize, this.dmdSize = dmdSize);
	}

	public int getNumberOfMasks() {
		return numberOfMasks;
	}

	public void setNumberOfMasks(int numberOfMasks) {
		firePropertyChange("numberOfMasks", this.numberOfMasks, this.numberOfMasks = numberOfMasks);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		firePropertyChange("name", this.name, this.name = name);
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		firePropertyChange("filename", this.filename, this.filename = filename);
	}

}
