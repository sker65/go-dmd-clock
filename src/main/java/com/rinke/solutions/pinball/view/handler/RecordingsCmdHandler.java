package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class RecordingsCmdHandler extends AbstractListCmdHandler implements ViewBindingHandler {

	@Autowired private MessageUtil messageUtil;
	Map<String,Integer> recordingsPosMap = new HashMap<String, Integer>();

	public RecordingsCmdHandler(ViewModel vm) {
		super(vm);
	}
	
	public void setPlayingAni(Animation ani, int pos) {
		log.debug("set playing ani {}, {}", pos, ani);
		vm.playingAnis.clear();
		vm.playingAnis.add(ani);
		animationHandler.setAnimations(vm.playingAnis);
		animationHandler.setPos(pos);
		vm.setSelectedFrame(pos);
		vm.setDmdDirty(true);
	}
	
	public void setEnableHashButtons(boolean enabled) {
		boolean[] sel = Arrays.copyOf(vm.hashButtonSelected, vm.hashButtonSelected.length);
		if( !enabled )  {
			for (int i = 0; i < vm.hashButtonSelected.length; i++) {
				sel[i] = false;
			}
			vm.setHashButtonSelected(sel);
		}
		vm.setHashButtonsEnabled(enabled);
	}
	
	private EditMode immutable[] = { EditMode.FIXED };

	public void onSelectedRecordingChanged(Animation o, Animation a) {
			log.info("onRecordingSelectionChanged: {}", a);
			Animation current = o;
			if( current == null && a == null ) return;
			if(a!= null && current != null && a.getDesc().equals(current.getDesc())) return;
			if( current != null ) recordingsPosMap.put(current.getDesc(), current.actFrame);
			if( a != null) {
				vm.cutInfo.reset();
				vm.setSelection(null);;
				
				vm.setSelectedScene(null);
				
				vm.setMaskEnabled(true);
				vm.setMaskSpinnerEnabled(true);
				
				vm.availableEditModes.clear();
				vm.availableEditModes.addAll(Arrays.asList(immutable));
				// sani check for recordings edit mode
				if( !EditMode.FIXED.equals(a.getEditMode()) ) a.setEditMode(EditMode.FIXED);
				vm.setSelectedEditMode(a.getEditMode());
				
				setEnableHashButtons(true);

				setPlayingAni(a, recordingsPosMap.getOrDefault(a.getDesc(), 0));

				int numberOfPlanes = a.getRenderer().getNumberOfPlanes();
				if( numberOfPlanes == 5) {
					numberOfPlanes = 4;
				}
				if (numberOfPlanes == 3) {
					numberOfPlanes = 2;
					//TODO v.goDmdGroup.transitionCombo.select(1);
				} else {
					//TODO v.goDmdGroup.transitionCombo.select(0);
				}

				
				//onColorMaskChecked(a.getEditMode()==EditMode.COLMASK);// doesnt fire event?????
				vm.dmd.setNumberOfSubframes(numberOfPlanes);
				
				vm.setNumberOfPlanes(vm.useGlobalMask?1:numberOfPlanes);

				Set<Bookmark> set = vm.bookmarksMap.get(a.getDesc());
				vm.bookmarks.clear();
				if( set != null ) {
					vm.bookmarks.addAll(set);
				}
				vm.setBookmarkComboEnabled(true);
			} else {
				//selectedRecording.set(null);
				//v.recordingsListViewer.setSelection(StructuredSelection.EMPTY);
				vm.bookmarks.clear();
				vm.setBookmarkComboEnabled(false);
			}
			
			//TODO v.goDmdGroup.updateAniModel(a);
			
			vm.setBtnDelBookmarkEnabled(a!=null);
			vm.setBtnNewBookmarkEnabled(a!=null);
			vm.setDeleteRecordingEnabled(a != null);
			
			vm.setBtnAddKeyframeEnabled(a != null);
			vm.setBtnAddFrameSeqEnabled(a!=null && vm.selectedFrameSeq != null);
			vm.setBtnAddEventEnabled(a != null);
	}
	
	public void onDeleteRecording() {
		Animation a = vm.selectedRecording;
		ArrayList<String> res = new ArrayList<>();
		if( a!=null) {
			for( PalMapping pm : vm.keyframes.values()) {
				if( a.getDesc().equals(pm.animationName) ) {
					res.add( pm.name );
				}
			}
		}
		if( res.isEmpty() ) {
			vm.bookmarksMap.remove(vm.selectedRecording.getDesc());
			onRemove(vm.selectedRecording, vm.recordings);
		} else {
			messageUtil.warn("Recording cannot be deleted", "It is used by "+res);
		}
	}

	public void onSortRecording() {
		onSortAnimations(vm.recordings);
	}
	
	private void updatePalMappingsRecordingNames(String oldKey, String newKey) {
		if( StringUtils.equals(oldKey, newKey) ) return;
		vm.keyframes.values().forEach(p->{
			if( p.animationName != null && p.animationName.equals(oldKey)) {
				p.animationName = newKey;
			}
		});
	}

	public void onRenameRecording(String oldName, String newName){
		updateAnimationMapKey(oldName, newName, vm.recordings);
		updatePalMappingsRecordingNames(oldName, newName);
		//ed.project.recordingNameMap.put(oldName, newName);
		vm.setDirty(true);
	}
	
}
