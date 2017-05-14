package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.viewers.StructuredSelection;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.TypedLabel;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Slf4j
@Bean
public class RecordingsHandler extends ViewHandler {

	public RecordingsHandler(ViewModel vm, Model model, CmdDispatcher d) {
		super(vm, model,d);
		model.recordings.addObserver((o,a)->populate());
	}

	public void populate() {
		vm.recordings.clear();
		for(Animation c: model.recordings.values()) {
			vm.recordings.add( new TypedLabel("fixed", c.getDesc()));
		}
	}
	
	public void onSortRecordings() {
		ArrayList<TypedLabel> list = new ArrayList<>(vm.recordings);
		Collections.sort(list, (o1,o2)->o1.label.compareTo(o2.label));
		vm.recordings.clear();
		vm.recordings.addAll(list);	
	}
	
	public void onSelectedRecordingChanged(TypedLabel oldVal, TypedLabel newVal) {
		log.info("onRecordingSelectionChanged: {}", newVal);
		Optional<Animation> currentRecording = model.getRecording(oldVal);
		Optional<Animation> newRecording = model.getRecording(newVal);
		
		if( currentRecording.isPresent() ) model.recordingsPosMap.put(oldVal.label, vm.actFrame);
		if( newRecording.isPresent() ) {		
			Animation rec = newRecording.get();
			vm.setDmdSelection(null); //dmdWidget.resetSelection();
			vm.setSelectedScene(null);

			vm.setMaskOnEnabled(true);
			vm.setMaskVisible(false);
			vm.setMaskSpinnerEnabled(true);
			vm.availableEditModes.clear();
			vm.availableEditModes.addAll(Arrays.asList( EditMode.FIXED ));
			vm.setHashButtonsEnabled(true);
			vm.setActFrame(model.recordingsPosMap.getOrDefault(rec.getDesc(), 0));
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
		}
		//goDmdGroup.updateAniModel(a);
		//btnRemoveAni.setEnabled(a != null);
		vm.setAddColSceneEnabled(newRecording.isPresent() && vm.selectedFrameSeq!=null);
		vm.setAddEventEnabled(newRecording.isPresent());
		vm.setAddPaletteSwitchEnabled(newRecording.isPresent());
		vm.setDeleteRecordingEnabled(newRecording.isPresent());
	}

}
