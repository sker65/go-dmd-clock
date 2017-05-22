package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.viewers.StructuredSelection;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.util.ObservableSet;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.TypedLabel;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Slf4j
@Bean
public class RecordingsHandler extends ViewHandler {

	public RecordingsHandler(ViewModel vm, Model model, CmdDispatcher d) {
		super(vm, model,d);
	}
	
	public void init() {
		model.recordings.addObserver((o,a)->populate());
	}

	public void populate() {
		vm.recordings.clear();
		for(Animation c: model.recordings.values()) {
			vm.recordings.add( c );
		}
	}
	
	public void onRecordingRenamed(String oldName, String newName) {
		if( StringUtils.equals(oldName, newName)) return;
		// update recording itself
		Animation animation = model.recordings.remove(oldName);
		animation.setDesc(newName);
		model.recordings.put(newName, animation);
		// update bookmarks
		ObservableSet<Bookmark> set = model.bookmarksMap.remove(oldName);
		model.bookmarksMap.put(newName, set);
		// update pos map
		Integer pos = model.recordingsPosMap.remove(oldName);
		if( pos != null ) model.recordingsPosMap.put(newName, pos);
		// update palmappings
		model.palMappings.forEach(p->{
			if( p.animationName.equals(oldName) ) {
				p.animationName = newName;
			}
		});
		vm.setDirty(true);
	}
	
	public void onSortRecordings() {
		ArrayList<Animation> list = new ArrayList<>(vm.recordings);
		Collections.sort(list, (o1,o2)->o1.getDesc().compareTo(o2.getDesc()));
		vm.recordings.clear();
		vm.recordings.addAll(list);	
	}
	
	public void onSelectedRecordingChanged(Animation oldVal, Animation newVal) {
		log.info("onRecordingSelectionChanged: {}", newVal);
		Optional<Animation> currentRecording = Optional.ofNullable(oldVal);
		Optional<Animation> newRecording = Optional.ofNullable(newVal);
		
		if( currentRecording.isPresent() ) model.recordingsPosMap.put(oldVal.getDesc(), vm.selectedFrame);
		if( newRecording.isPresent() ) {		
			Animation rec = newRecording.get();
			vm.setDmdSelection(null); //dmdWidget.resetSelection();
			vm.setSelectedScene(null);

			vm.setMaskOnEnabled(true);
			vm.setMaskVisible(false);
			vm.setMaskNumberEnabled(true);
			vm.availableEditModes.replace(Arrays.asList( EditMode.FIXED ));
			vm.setSelectedFrame(model.recordingsPosMap.getOrDefault(rec.getDesc(), 0));
			vm.setSelectedEditMode(rec.getEditMode());
			// bound to select dmdAni
			// playing ani
			// dmd.setNumberOfSubframes(numberOfPlanes);
			// paletteTool.setNumberOfPlanes(useGlobalMask?1:numberOfPlanes);
			// Set<Bookmark> set = project.bookmarksMap.get(a.getDesc());
			// if( set != null ) bookmarkComboViewer.setInput(set);
			// else bookmarkComboViewer.setInput(Collections.EMPTY_SET);
			// bookmarkComboViewer.setInput(Collections.EMPTY_SET);
			vm.setPlayingAni(rec);
		} else {
			vm.setPlayingAni(null);
		}
		//goDmdGroup.updateAniModel(a);
		//btnRemoveAni.setEnabled(a != null);
		vm.setAddColSceneEnabled(newRecording.isPresent() && vm.selectedFrameSeq!=null);
		vm.setAddEventEnabled(newRecording.isPresent());
		vm.setAddPaletteSwitchEnabled(newRecording.isPresent());
		vm.setDeleteRecordingEnabled(newRecording.isPresent());
	}
	
	public void onDeleteRecording(Animation recToDelete) {
		// keep track of input files
		String key = recToDelete.getDesc();
		String filename = recToDelete.getName();
		model.recordings.remove(key);
		model.bookmarksMap.remove(key);
		model.inputFiles.remove(filename);
		populate();
		if( recToDelete.equals(vm.playingAni)) {
			vm.setPlayingAni(null);
		}
		vm.setSelectedRecording(null);
		vm.setDirty(true);
	}

}
