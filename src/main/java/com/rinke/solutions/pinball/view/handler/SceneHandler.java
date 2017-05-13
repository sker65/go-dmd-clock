package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.viewers.StructuredSelection;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.TypedLabel;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Slf4j
@Bean
public class SceneHandler extends ViewHandler {
	
	@Autowired
	PaletteHandler paletteHandler;

	public SceneHandler(ViewModel vm, Model model, CmdDispatcher d) {
		super(vm, model,d);
		model.scenes.addObserver((o,a)->populate());
	}

	public void populate() {
		vm.scenes.clear();
		for(Animation c: model.scenes.values()) {
			vm.scenes.add( new TypedLabel(c.getEditMode().label, c.getDesc()));
		}
	}
	private <T extends Animation> void onSortAnimations(ObservableMap<String, T> map) {
		ArrayList<Entry<String, T>> list = new ArrayList<>(map.entrySet());
		Collections.sort(list, new Comparator<Entry<String, T>>() {

			@Override
			public int compare(Entry<String, T> o1, Entry<String, T> o2) {
				return o1.getValue().getDesc().compareTo(o2.getValue().getDesc());
			}
		});
		map.clear();
		for (Entry<String, T> entry : list) {
			map.put(entry.getKey(), (T)entry.getValue());
		}
	}

	public void onSortScenes() {
		ArrayList<TypedLabel> list = new ArrayList<>(vm.scenes);
		Collections.sort(list, (o1,o2)->o1.label.compareTo(o2.label));
		vm.scenes.clear();
		vm.scenes.addAll(list);	
	}
	
	public void onSelectedSceneChanged(TypedLabel oldVal, TypedLabel newVal) {
		log.info("onSelectedSceneChanged: {}", newVal);
		Optional<CompiledAnimation> oldScene = model.getScene(oldVal);
		Optional<CompiledAnimation> newScene = model.getScene(newVal);
		
		if( oldScene.isPresent() ) model.scenesPosMap.put(oldVal.label, vm.actFrame);
		if( newScene.isPresent() ) {	
			CompiledAnimation a = newScene.get();
			vm.setDmdSelection(null); //dmdWidget.resetSelection();
			vm.setSelectedRecording(null);
			vm.setMaskOnEnabled(a.getEditMode().equals(EditMode.FOLLOW));
			vm.setMaskVisible(false);
			vm.setMaskSpinnerEnabled(true);
			vm.availableEditModes.clear();
			vm.availableEditModes.addAll(Arrays.asList( EditMode.REPLACE, EditMode.COLMASK, EditMode.FOLLOW ));
			vm.setHashButtonsEnabled(false);
			vm.setActFrame(model.scenesPosMap.getOrDefault(a.getDesc(), 0));
			vm.setSelectedEditMode(a.getEditMode());
			vm.setSelectedPalette(paletteHandler.getPaletteByIndex(a.getPalIndex()));
			vm.setPlayingAni(newScene.get());
		}	
		vm.setDeleteSceneEnabled(newScene.isPresent());
		vm.setAddColSceneEnabled(false);
		vm.setAddEventEnabled(false);
		vm.setAddPaletteSwitchEnabled(false);
		vm.setDrawingEnabled(newScene.isPresent());
	}

}
