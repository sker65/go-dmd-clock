package com.rinke.solutions.pinball.view.handler;

import java.util.Arrays;
import java.util.Collections;
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
	}

	public void populate() {
		vm.recordings.clear();
		for(Animation c: model.recordings.values()) {
			vm.recordings.add( new TypedLabel("fixed", c.getDesc()));
		}
	}
	
	public void onSelectedRecordingChanged(TypedLabel oldVal, TypedLabel newVal) {
		log.info("onRecordingSelectionChanged: {}", newVal);
		Animation current = oldVal==null?null:model.recordings.get(oldVal.label);
		Animation a = newVal==null?null:model.recordings.get(newVal.label);
		
		if( current == null && a == null ) return;
		if(a!= null && current != null && a.getDesc().equals(current.getDesc())) return;
		if( current != null ) model.recordingsPosMap.put(oldVal.label, vm.actFrame);
		if( a != null) {		
			vm.setDmdSelection(null); //dmdWidget.resetSelection();
			vm.setSelectedScene(null);
			vm.setMaskOnEnabled(true);
			vm.setMaskSpinnerEnabled(true);
			vm.availableEditModes.clear();
			vm.availableEditModes.addAll(Arrays.asList( EditMode.FIXED ));
			vm.setHashButtonsEnabled(true);
			vm.setActFrame(model.recordingsPosMap.getOrDefault(a.getDesc(), 0));
			vm.setSelectedEditMode(a.getEditMode());
			// bound to select dmdAni
			// playing ani
			// dmd.setNumberOfSubframes(numberOfPlanes);
			// paletteTool.setNumberOfPlanes(useGlobalMask?1:numberOfPlanes);
			// Set<Bookmark> set = project.bookmarksMap.get(a.getDesc());
			// if( set != null ) bookmarkComboViewer.setInput(set);
			// else bookmarkComboViewer.setInput(Collections.EMPTY_SET);
			// bookmarkComboViewer.setInput(Collections.EMPTY_SET);
		}
		vm.setPlayingAni(a);
		//goDmdGroup.updateAniModel(a);
		//btnRemoveAni.setEnabled(a != null);
		vm.setAddColSceneEnabled(a != null && vm.selectedFrameSeq!=null);
		vm.setAddEventEnabled(a!=null);
		vm.setAddPaletteSwitchEnabled(a!=null);
	}

}
