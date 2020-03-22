package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.ui.NamePrompt;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.View;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class KeyframeHandler extends AbstractCommandHandler implements ViewBindingHandler {

	@Autowired
	MessageUtil messageUtil;
	@Autowired HashCmdHandler hashCmdHandler;
	@Autowired MaskHandler maskHandler;
	@Autowired View namePrompt;

	public KeyframeHandler(ViewModel vm) {
		super(vm);
	}

	public void onSelectedSpinnerDeviceIdChanged(int o, int n) {
		if( vm.selectedKeyFrame != null && vm.selectedKeyFrame.switchMode.equals(SwitchMode.EVENT)) {
			int duration = (n << 8) + vm.selectedSpinnerEventId;
			vm.selectedKeyFrame.durationInMillis = duration;
			vm.setDuration(duration);
		}
	}

	public void onSelectedSpinnerEventIdChanged(int o, int n) {
		if( vm.selectedKeyFrame != null && vm.selectedKeyFrame.switchMode.equals(SwitchMode.EVENT)) {
			int duration = (vm.selectedSpinnerDeviceId << 8) + n; 
			vm.selectedKeyFrame.durationInMillis = duration;
			vm.setDuration(duration);
		}
	}
	
	public void onAddKeyframe() {
		onAddKeyframe(null);
	}
	
	public void onAddKeyframe(SwitchMode switchMode) {
		// retrieve switch mode from selected scene edit mode!!
		Animation ani = vm.selectedFrameSeq;
		EditMode editMode = ani==null?null:ani.getEditMode();
		if( switchMode == null ) switchMode = getSwitchModeFromEditMode(editMode);

		String prompt = getName(switchMode, ani);
		do {
			NamePrompt namePrompt = (NamePrompt) this.namePrompt;
			namePrompt.setItemName("Keyframe");
			namePrompt.setPrompt(prompt);
			namePrompt.open();
			if( namePrompt.isOkay() ) prompt = namePrompt.getPrompt();
			else return;
			
			if (SwitchMode.EVENT.equals(switchMode)) {
				prompt = "!"+ namePrompt.getPrompt();
			}
			
			if( vm.keyframes.containsKey(prompt) ) {
				messageUtil.error("Keyframe Name exists", "A keyframe '"+prompt+"' already exists");
			}
		} while(vm.keyframes.containsKey(prompt));
		
		if (vm.selectedHashIndex == -1) {
			messageUtil.error("No Hash Selected", "Please select a hash");
			return;
		} 
		
		PalMapping palMapping = new PalMapping(0, prompt );
		palMapping.setDigest(vm.hashes.get(vm.selectedHashIndex));
	
		if( ani != null ) {
			palMapping.palIndex = ani.getPalIndex();
			if( !( SwitchMode.EVENT.equals(switchMode) || SwitchMode.PALETTE.equals(switchMode) ) ) // not needed for these two 
				palMapping.frameSeqName = ani.getDesc();
		} else {
			palMapping.palIndex = vm.selectedPalette.index;
		}
		
		palMapping.animationName = vm.selectedRecording.getDesc();
		palMapping.switchMode = switchMode;
		palMapping.frameIndex = vm.selectedRecording.actFrame;
		palMapping.hashIndex = vm.selectedHashIndex;
		
		if( switchMode.equals(SwitchMode.EVENT)) {
			palMapping.durationInMillis = (vm.selectedSpinnerDeviceId<<8) + vm.selectedSpinnerEventId;
		} else {
			if (SwitchMode.PALETTE.equals(switchMode)) {
				vm.setDuration(0);
				vm.setSelectedFrameSeq(null);
			}	
			palMapping.durationInMillis = vm.duration;
		}
		if (vm.showMask) {
			maskHandler.commitMaskIfNeeded(true);
			palMapping.withMask = true;
			palMapping.maskNumber = vm.selectedMaskNumber;
			// mask is used now -> lock it
			if( !vm.masks.get(vm.selectedMaskNumber).locked ) {
				vm.masks.get(vm.selectedMaskNumber).locked = true;
				vm.dmd.getFrame().mask.locked = true;
				vm.setDmdDirty(true);
			}
			vm.setDetectionMaskActive(true);
		} 
		else if( editMode != null && editMode.enableLayerMask && vm.selectedScene != null ) {
			palMapping.withMask = true;
			palMapping.maskNumber = vm.selectedMaskNumber;
			vm.selectedScene.getMask(vm.selectedMaskNumber).locked = true;
			//vm.setDetectionMaskActive(false);
			palMapping.targetFrameIndex = 0; // how will this look like
		}
		String duplicateName = checkForDuplicateKeyFrames(palMapping);
		if (duplicateName == null) {
			vm.keyframes.put(palMapping.name,palMapping);
		} else {
			messageUtil.warn("duplicate hash", "There is already Keyframe \"" + duplicateName + "\" that uses the same hash");
		}
		vm.setSelectedKeyFrame(palMapping);
		vm.setSelectedPalette(vm.paletteMap.values().stream()
				.filter(p->p.type.equals(PaletteType.DEFAULT)).findFirst().orElse(vm.selectedPalette));
	}
	
	/**
	 * creates unique name
	 * @param switchMode 
	 * @param ani selected switch scene 
	 * @return name
	 */
	 String getName(SwitchMode switchMode, Animation ani) {
//		String name = "KeyFrame " + ( ani!=null 
//				&&  !switchMode.equals(SwitchMode.PALETTE) 
//				&& !switchMode.equals(SwitchMode.EVENT) ? ani.getDesc():Integer.toString(vm.keyframes.size()+1) );
	 	String name = "";
		if (ani!=null && !switchMode.equals(SwitchMode.PALETTE) && !switchMode.equals(SwitchMode.EVENT)) {
			name = name + ani.getDesc();
		} else {
			name = "KeyFrame ";
		}
		int i = 0;
		String res = name;
		while( vm.keyframes.containsKey(res)) {
			i++;
			res = name + " " + Integer.toString(i);
		}
		return res;
	}

	private SwitchMode getSwitchModeFromEditMode(EditMode editMode) {
		SwitchMode switchMode = SwitchMode.PALETTE;
		switch(editMode) {
			case COLMASK: switchMode = SwitchMode.ADD; break;
			case COLMASK_FOLLOW: switchMode = SwitchMode.FOLLOW; break;
			case REPLACE: switchMode = SwitchMode.REPLACE; break;
			case LAYEREDCOL: switchMode = SwitchMode.LAYEREDCOL; break;
			case REPLACE_FOLLOW: switchMode = SwitchMode.FOLLOWREPLACE; break;
			case LAYEREDREPLACE: switchMode = SwitchMode.LAYEREDREPLACE; break;
			default: break;
		}
		return switchMode;
	}
	
	private EditMode getEditModeFromSwitchMode(SwitchMode switchMode) {
		EditMode editMode = EditMode.FIXED;
		switch(switchMode) {
			case ADD: editMode = EditMode.COLMASK; break;
			case REPLACE: editMode = EditMode.REPLACE; break;
			case LAYEREDCOL: editMode = EditMode.LAYEREDCOL; break;
			case FOLLOWREPLACE: editMode = EditMode.REPLACE_FOLLOW; break;
			case LAYEREDREPLACE: editMode = EditMode.LAYEREDREPLACE; break;
			case FOLLOW: editMode = EditMode.COLMASK_FOLLOW; break;
			default: break;
		}
		return editMode;
	}

	
	public void onFixPaletteAndMode() {
		int res = messageUtil.warn(0, "Warning",
				"Use only on corrupted projects !", 
				"This function synchronizes the mode and palette of the scenes and the keyframes.\n Take the mode and palette settings from :",
				new String[]{"Cancel", "Scenes", "Keyframes"},2);
		if( res == 0 ) return;
		for( PalMapping pm : vm.keyframes.values()) {
			if (pm.frameSeqName != null) {
				if (res == 2) {
					vm.setSelectedScene(vm.scenes.get(pm.frameSeqName));
					vm.selectedScene.setPalIndex(pm.palIndex);
					vm.selectedScene.setEditMode(getEditModeFromSwitchMode(pm.switchMode));
				} else if (res == 1) {
					vm.setSelectedScene(vm.scenes.get(pm.frameSeqName));
					pm.switchMode = getSwitchModeFromEditMode(vm.selectedScene.getEditMode());
					pm.palIndex = vm.selectedScene.getPalIndex();
					vm.setSelectedKeyFrame(pm);
				}
			}
		}
	}
	
	public void onCheckKeyframe() {
		List<String> res = new ArrayList<>();
		for( PalMapping pm : vm.keyframes.values()) {
			//pm.maskNumber
			vm.setDetectionMaskActive(pm.withMask);
			if( pm.withMask ) {
				vm.dmd.setMask(vm.masks.get(pm.maskNumber));
				vm.setSelectedMaskNumber(pm.maskNumber);
			}
			hashCmdHandler.updateHashes(vm.dmd.getFrame());
			for (int idx = 0;idx < vm.hashes.size();idx++) {
				if(Arrays.equals(pm.crc32, vm.hashes.get(idx))) {
					res.add(" "+pm.name);
					break;
				}
			}
		}
		if (res.size() != 0)
			messageUtil.warn("Keyframe found", "The selected frame gets triggered by Keyframe:\n"+res);
		else
			messageUtil.warn("No Keyframe found", "No Keyframe found for the selected frame.");
	}


	String checkForDuplicateKeyFrames(PalMapping palMapping) {
		for (PalMapping p : vm.keyframes.values()) {
			if (Arrays.equals(p.crc32, palMapping.crc32)) {
				if (!palMapping.switchMode.equals(SwitchMode.EVENT) && !p.switchMode.equals(SwitchMode.EVENT))
					return p.name;
				if (palMapping.switchMode.equals(p.switchMode))
					return p.name;
			}
		}
		return null;
	}

	public void onDeleteKeyframe() {
		vm.keyframes.remove(vm.selectedKeyFrame.name);
		checkReleaseMask();
	}
	/**
	 * checks all pal mappings and releases masks if not used anymore
	 */
	void checkReleaseMask() {
		HashSet<Integer> useMasks = new HashSet<>();
		// collect mask numbers for all masks that are referenced by keyframes
		for (PalMapping p : vm.keyframes.values()) {
			if (p.withMask) {
				useMasks.add(p.maskNumber);
			}
		}
		for (int i = 0; i < vm.masks.size(); i++) {
			// if the not used mask is actually show, force screen update
			if( vm.masks.get(i).locked && !useMasks.contains(i) && vm.selectedMaskNumber == i && vm.showMask ) {
				vm.masks.get(i).locked = useMasks.contains(i);
				vm.setDmdDirty(true);
			}
			// update locked flag according to usage
			vm.masks.get(i).locked = useMasks.contains(i);
		}
		
		vm.setDetectionMaskActive(vm.selectedScene != null ? vm.selectedScene.getEditMode().enableDetectionMask : false);
	}
	
	public void onSetKeyframePalette() {
		if (vm.selectedKeyFrame != null && vm.selectedPalette != null) {
			vm.selectedKeyFrame.palIndex = vm.selectedPalette.index;
			log.info("change pal index in Keyframe {} to {}", vm.selectedKeyFrame.name, vm.selectedPalette.index);
		}
	}
	
	public void onSortKeyFrames() {
		ArrayList<Entry<String, PalMapping>> list = new ArrayList<>(vm.keyframes.entrySet());
		Collections.sort(list, new Comparator<Entry<String, PalMapping>>() {
	
			@Override
			public int compare(Entry<String, PalMapping> o1, Entry<String, PalMapping> o2) {
				return o1.getValue().name.compareTo(o2.getValue().name);
			}
		});
		vm.keyframes.clear();
		for (Entry<String, PalMapping> entry : list) {
			vm.keyframes.put(entry.getKey(), (PalMapping)entry.getValue());
		}
	}

	public void onSelectedKeyFrameChanged(PalMapping old, PalMapping nk) {
		if( nk != null) {

			log.debug("selected new palMapping {}", nk);

			// current firmware always checks with and w/o mask
			// btnMask.setSelection(selectedPalMapping.withMask);
			// btnMask.notifyListeners(SWT.Selection, new Event());

			vm.setDuration(nk.durationInMillis);

			if( nk.switchMode.equals(SwitchMode.EVENT)) {
				vm.setSelectedSpinnerDeviceId(nk.durationInMillis >> 8);
				vm.setSelectedSpinnerEventId(nk.durationInMillis & 0xFF);
			}
			
			vm.setSelectedScene(null);
			Animation rec = vm.recordings.isEmpty() ? null : vm.recordings.values().iterator().next(); // get the first one
			if( !StringUtils.isEmpty(nk.animationName )) {
				if( vm.recordings.containsKey(nk.animationName) ) {
					rec = vm.recordings.get(nk.animationName);
				} else {
					log.warn("keyframe has invalid reference to recording '{}'", nk.animationName);
				}
			} else {
				log.warn("keyframe has invalid empty reference: '{}'", nk.animationName);
			}
			
			vm.setSelectedRecording(rec);

			vm.setSelectedPaletteByIndex(nk.palIndex);
			
			if (nk.frameSeqName != null) {
				vm.setSelectedFrameSeq(vm.scenes.get(nk.frameSeqName));
				vm.setDurationEnabled(false);
				vm.setFetchDurationEnabled(false);
			} else {
				vm.setSelectedFrameSeq(null);
				vm.setDurationEnabled(true);
				vm.setFetchDurationEnabled(true);
			}

			vm.setSelectedFrame(nk.frameIndex);
			vm.setDetectionMaskActive(nk.withMask);
			if( nk.withMask ) {
				vm.dmd.setMask(vm.masks.get(nk.maskNumber));
				vm.setSelectedMaskNumber(nk.maskNumber);
			}
			
			hashCmdHandler.updateHashes(vm.dmd.getFrame());
			vm.setHashVal(HashCmdHandler.getPrintableHashes(nk.crc32));

			if(Arrays.equals(nk.crc32, vm.hashes.get(nk.hashIndex))) {
				vm.setSelectedHashIndex(nk.hashIndex);
			} else {
				for (int idx = 0;idx < vm.hashes.size();idx++) {
					if(Arrays.equals(nk.crc32, vm.hashes.get(idx))) {
						vm.setSelectedHashIndex(idx);
						nk.hashIndex = idx;
						break;
					}
				}
			}
			
			if( vm.selectedRecording!=null )
				vm.saveTimeCode = (int) vm.selectedRecording.getTimeCode(nk.frameIndex);
		} else {
			vm.setSelectedKeyFrame(null);
		}
		vm.setBtnSetHashEnabled(nk != null);
		vm.setBtnPreviewNextEnabled(nk != null);
		vm.setBtnPreviewPrevEnabled(nk != null);
		vm.setDeleteKeyFrameEnabled(nk != null);
		vm.setSetKeyFramePalEnabled(nk != null && SwitchMode.PALETTE.equals(nk.switchMode));
	}
	
	private static final int FRAME_RATE = 40;
	
	public void onFetchDuration() {
		vm.setDuration(vm.lastTimeCode - vm.saveTimeCode);
	}
	
	public void onDurationChanged(int o, int n) {
		if (vm.selectedKeyFrame != null) {
			log.debug("setting duration for {} to {}", vm.selectedKeyFrame.name, n);
			vm.selectedKeyFrame.durationInMillis = n;
			vm.selectedKeyFrame.durationInFrames = (int) vm.selectedKeyFrame.durationInMillis / FRAME_RATE;
		}
	}

	public void onSelectedFrameSeqChanged(Animation old, Animation ani) {
		vm.setBtnAddFrameSeqEnabled(ani != null && vm.selectedRecording!=null);
	}

	byte[] saveGetHash(int idx) {
		return idx>=0 && idx<vm.hashes.size() ? vm.hashes.get(idx) : null;
	}
	
	public void onSetHash() {
		if( vm.selectedHashIndex != -1 ) {
			byte[] hash = saveGetHash(vm.selectedHashIndex);
			if( vm.selectedKeyFrame != null ) {
				vm.selectedKeyFrame.setDigest(hash);
				vm.selectedKeyFrame.hashIndex = vm.selectedHashIndex;
			} else {
				if( vm.selectedEditMode.haveLocalMask ) {
					// Update hash in scene and lock mask (for scene masks)
					vm.selectedScene.getActualFrame().setHash(hash);
					vm.setHashVal(HashCmdHandler.getPrintableHashes(hash));
					if( vm.detectionMaskActive ) {
						maskHandler.commitMaskIfNeeded(true);
						vm.dmd.getFrame().mask.locked = true;
						vm.setDmdDirty(true);
					}
				}
			}
		}
	}

}
