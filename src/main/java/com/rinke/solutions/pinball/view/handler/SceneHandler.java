package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.util.MessageUtil;
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
	@Autowired
	KeyFrameHandler keyFrameHandler;
	@Autowired
	private MessageUtil messageUtil;

	public SceneHandler(ViewModel vm, Model model, CmdDispatcher d) {
		super(vm, model,d);
		model.scenes.addObserver((o,a)->populate());
	}

	public void populate() {
		vm.scenes.clear();
		for(CompiledAnimation c: model.scenes.values()) {
			vm.scenes.add(c);
		}
	}
	
	/*private <T extends Animation> void onSortAnimations(ObservableMap<String, T> map) {
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
	}*/

	public void onSceneRenamed(String oldName, String newName) {
		if( StringUtils.equals(oldName, newName)) return;
		CompiledAnimation cani = model.scenes.remove(oldName);
		cani.setDesc(newName);
		model.scenes.put(newName, cani);
		// update pos map
		Integer pos = model.scenesPosMap.remove(oldName);
		if( pos != null ) model.scenesPosMap.put(newName, pos);
		// update palmappings
		model.palMappings.forEach(p->{
			if( p.frameSeqName.equals(oldName) ) {
				p.frameSeqName = newName;
			}
		});
		vm.setDirty(true);
	}

	public void onSortScenes() {
		ArrayList<CompiledAnimation> list = new ArrayList<>(vm.scenes);
		Collections.sort(list, (o1,o2)->o1.getDesc().compareTo(o2.getDesc()));
		vm.scenes.clear();
		vm.scenes.addAll(list);	
	}
	
	public void onSelectedSceneChanged(CompiledAnimation oldVal, CompiledAnimation newVal) {
		log.info("onSelectedSceneChanged: {}", newVal);
		Optional<CompiledAnimation> oldScene = Optional.ofNullable(oldVal);
		Optional<CompiledAnimation> newScene = Optional.ofNullable(newVal);
		
		if( oldScene.isPresent() ) model.scenesPosMap.put(oldVal.getDesc(), vm.selectedFrame);
		if( newScene.isPresent() ) {	
			CompiledAnimation a = newScene.get();
			vm.setDmdSelection(null); //dmdWidget.resetSelection();
			vm.setSelectedRecording(null);
			vm.setMaskOnEnabled(a.getEditMode().equals(EditMode.FOLLOW));
			vm.setMaskVisible(false);
			vm.setMaskNumberEnabled(true);
			vm.availableEditModes.replace(Arrays.asList( EditMode.REPLACE, EditMode.COLMASK, EditMode.FOLLOW ));
			vm.setSelectedFrame(model.scenesPosMap.getOrDefault(a.getDesc(), 0));
			vm.setSelectedEditMode(a.getEditMode());
			vm.setSelectedPalette(paletteHandler.getPaletteByIndex(a.getPalIndex()));
			vm.setPlayingAni(newScene.get());
		} else {
			vm.setPlayingAni(null);
		}
		vm.setDeleteSceneEnabled(newScene.isPresent());
		vm.setAddColSceneEnabled(false);
		vm.setAddEventEnabled(false);
		vm.setAddPaletteSwitchEnabled(false);
		vm.setDrawingEnabled(newScene.isPresent());
	}
	
	private boolean hasReferenceToScene(SwitchMode sm) {
		return sm.equals(SwitchMode.ADD) || sm.equals(SwitchMode.FOLLOW) || sm.equals(SwitchMode.REPLACE);
	}
	
	public void onDeleteScene(Animation aniToDelete) {
		// check references in key frames
		String key = aniToDelete.getDesc();
		boolean reallyRemove = true;
		List<PalMapping> refs = new ArrayList<>();
		for( PalMapping p : model.palMappings ) {
			if( hasReferenceToScene(p.switchMode) ) {
				if( p.frameSeqName.equals(key)) {
					refs.add(p);
				}
			}
		}
		if( !key.isEmpty() ) {
			int r = messageUtil.warn(SWT.ICON_WARNING | SWT.YES | SWT.NO,
					"Scene has KeyFrame refrences", "The scene you're about to delete is referenced in keyframe: "
			+refs.stream().map(p->p.frameSeqName+", ")
			+"\nIf you proceed these KeyFrames will deleted as well. Really delete?");
			if( r==SWT.NO ) {
				reallyRemove=false;
			} else {
				model.palMappings.removeAll(refs);
				keyFrameHandler.populate();
			}
		}
		if( reallyRemove ) {
			model.bookmarksMap.remove(key);
			model.scenes.remove(aniToDelete);
			populate();
			if( vm.playingAni.equals(aniToDelete)) {
				vm.setPlayingAni(null);
			}
			vm.setSelectedScene(null);
			vm.setDirty(true);
		}
	}

}
