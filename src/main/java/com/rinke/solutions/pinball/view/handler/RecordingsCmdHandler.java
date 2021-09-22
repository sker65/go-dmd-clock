package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.animation.RawAnimation;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class RecordingsCmdHandler extends AbstractListCmdHandler implements ViewBindingHandler {

	@Autowired private MessageUtil messageUtil;
	@Autowired private HashCmdHandler hashCmdHandler;

	Map<String,Integer> recordingsPosMap = new HashMap<String, Integer>();

	public RecordingsCmdHandler(ViewModel vm) {
		super(vm);
	}
	
	private List<EditMode> immutable = Arrays.asList( EditMode.FIXED );

	public void onSelectedRecordingChanged(Animation o, Animation a) {
			log.info("onRecordingSelectionChanged: {}", a);
			//Animation current = o;
			if( o == null && a == null ) return;
			if(a!= null && o != null && a.getDesc().equals(o.getDesc())) return;
			if( o != null ) recordingsPosMap.put(o.getDesc(), o.actFrame);
			if( a != null) {
				vm.cutInfo.reset();
				vm.setSelection(null);
				
				vm.setSelectedScene(null);
				vm.setLinkVal("-");
				
				// sani check for recordings edit mode
				if( !EditMode.FIXED.equals(a.getEditMode()) ) a.setEditMode(EditMode.FIXED);

				if( o == null ) {
					vm.availableEditModes.replaceAll(immutable);
				}

				// for recordings we use global masks
				vm.setLayerMaskActive(false);
				vm.setLayerMaskEnabled(false);
				vm.setDetectionMaskEnabled(true);
				vm.setDetectionMaskActive(false);
				vm.setMaskSpinnerEnabled(true);
				
				vm.setSuggestedEditMode(EditMode.FIXED);
				vm.setSelectedEditMode(EditMode.FIXED);
				
				setEnableHashButtons(true);

				vm.setPreviewDMD(null);
				
				setPlayingAni(a, recordingsPosMap.getOrDefault(a.getDesc(), 0));
				if (vm.dmdSize.planeSize == vm.prjDmdSize.planeSize)
					vm.setDmdDirty(true);
				
				vm.setSelectedPalette(vm.paletteMap.values().stream()
						.filter(p->p.type.equals(PaletteType.DEFAULT)).findFirst().orElse(vm.selectedPalette));

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
				vm.dmd.setSize(a.width, a.height);
				vm.dmd.setNumberOfPlanes(numberOfPlanes);
				vm.setDmdSize(DmdSize.fromWidthHeight(a.width, a.height));
				vm.setPaletteToolPlanes(vm.detectionMaskActive || vm.layerMaskActive ? 1 :numberOfPlanes);
				animationHandler.forceRerender();

				Set<Bookmark> set = vm.bookmarksMap.get(a.getDesc());
				vm.bookmarks.clear();
				if( set != null ) {
					vm.bookmarks.addAll(set);
				}
				vm.setBookmarkComboEnabled(true);
			} else {
				vm.bookmarks.clear();
				vm.setBookmarkComboEnabled(false);
			}
			
			//TODO v.goDmdGroup.updateAniModel(a);
			
			vm.setBtnDelBookmarkEnabled(a!=null);
			vm.setBtnNewBookmarkEnabled(a!=null);
			vm.setDeleteRecordingEnabled(a!=null);
			vm.setBtnCheckKeyframeEnabled(a!=null);
			
			hashCmdHandler.updateKeyFrameButtons(a, vm.selectedFrameSeq, vm.selectedHashIndex);
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
			String filename = vm.selectedRecording.getName();
			int i = vm.inputFiles.indexOf(filename);
			onRemove(vm.selectedRecording, vm.recordings);
			// if not used anymore delete from file list
			boolean nameExists = false;
			for (Animation r: vm.recordings.values()) {
				String name = r.getName();
				if (name.equals(filename))
					nameExists = true;
			}
			if ( nameExists != true && (i != -1)) vm.inputFiles.remove(i);
			vm.setDirty(true);
		} else {
			messageUtil.warn("Recording cannot be deleted", "It is used by "+res);
		}
	}

	public void onSortRecording() {
		onSortAnimations(vm.recordings);
		vm.setDirty(true);
	}
	
	void updatePalMappingsRecordingNames(String oldKey, String newKey) {
		if( StringUtils.equals(oldKey, newKey) ) return;
		vm.keyframes.values().forEach(p->{
			if( p.animationName != null && p.animationName.equals(oldKey)) {
				p.animationName = newKey;
			}
		});
		vm.setDirty(true);
	}

	public void onRenameRecording(String oldName, String newName){
		updateAnimationMapKey(oldName, newName, vm.recordings);
		updatePalMappingsRecordingNames(oldName, newName);
		updateRecordingLinks(oldName, newName);
		vm.recordingNameMap.put(oldName, newName);
		vm.setDirty(true);
	}

	private void updateRecordingLinks(String oldName, String newName) {
		for(CompiledAnimation a: vm.scenes.values()) {
			RecordingLink link = a.getRecordingLink();
			if( link != null && link.associatedRecordingName.equals(oldName)) {
				link.associatedRecordingName = newName;
			}
		}
		vm.setDirty(true);
	}
}
