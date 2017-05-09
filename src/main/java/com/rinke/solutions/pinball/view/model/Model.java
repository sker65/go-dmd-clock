package com.rinke.solutions.pinball.view.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.util.ObservableMap;

@Bean
public class Model {
	public ObservableMap<String, CompiledAnimation> scenes = new ObservableMap<String, CompiledAnimation>(new LinkedHashMap<>());
	public ObservableMap<String, Animation> recordings = new ObservableMap<String, Animation>(new LinkedHashMap<>());
	
	public List<PalMapping> palMappings = new ArrayList<>();
	
	public Map<String,Integer> recordingsPosMap = new HashMap<String, Integer>();
	public Map<String,Integer> scenesPosMap = new HashMap<String, Integer>();
	
	public List<Mask> masks = new ArrayList<>();
	
	public Map<String,Set<Bookmark>> bookmarksMap = new HashMap<>();
}
