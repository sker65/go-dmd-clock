package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class KeyframeHandler extends AbstractCommandHandler implements ViewBindingHandler {

	@Autowired
	MessageUtil messageUtil;
	@Autowired HashCmdHandler hashCmdHandler;
	@Autowired MaskHandler maskHandler;

	public KeyframeHandler(ViewModel vm) {
		super(vm);
	}

	public void onSelectedSpinnerDeviceIdChanged(int o, int n) {
		if( vm.selectedKeyFrame != null ) {
			vm.selectedKeyFrame.durationInMillis = n << 8 +vm.selectedSpinnerEventId; 
		}
	}

	public void onSelectedSpinnerEventIdChanged(int o, int n) {
		if( vm.selectedKeyFrame != null ) {
			vm.selectedKeyFrame.durationInMillis = vm.selectedSpinnerDeviceId << 8 + n; 
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

		PalMapping palMapping = new PalMapping(0, getName(switchMode, ani) );
		palMapping.setDigest(vm.hashes.get(vm.selectedHashIndex));
	
		if( ani != null ) {
			palMapping.palIndex = ani.getPalIndex();
			palMapping.frameSeqName = ani.getDesc();
		} else {
			palMapping.palIndex = vm.selectedPalette.index;
		}
		
		palMapping.animationName = vm.selectedRecording.getDesc();
		palMapping.switchMode = switchMode;
		palMapping.frameIndex = vm.selectedRecording.actFrame;
		palMapping.hashIndex = vm.selectedHashIndex;
		
		if( switchMode.equals(SwitchMode.EVENT)) {
			palMapping.durationInMillis = vm.selectedSpinnerDeviceId<<8 + vm.selectedSpinnerEventId;
		} else {
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
		if (!checkForDuplicateKeyFrames(palMapping)) {
			vm.keyframes.put(palMapping.name,palMapping);
		} else {
			messageUtil.warn("duplicate hash", "There is already another Keyframe that uses the same hash");
		}
	}
	
	/**
	 * creates unique name
	 * @param switchMode 
	 * @param ani selected switch scene 
	 * @return name
	 */
	 String getName(SwitchMode switchMode, Animation ani) {
		String name = "KeyFrame " + ( ani!=null 
				&&  !switchMode.equals(SwitchMode.PALETTE) 
				&& !switchMode.equals(SwitchMode.EVENT) ? ani.getDesc():Integer.toString(vm.keyframes.size()+1) );
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
			default: break;
		}
		return switchMode;
	}

	boolean checkForDuplicateKeyFrames(PalMapping palMapping) {
		for (PalMapping p : vm.keyframes.values()) {
			if (Arrays.equals(p.crc32, palMapping.crc32))
				return true;
		}
		return false;
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

			vm.setSelectedHashIndex(nk.hashIndex);

			// current firmware always checks with and w/o mask
			// btnMask.setSelection(selectedPalMapping.withMask);
			// btnMask.notifyListeners(SWT.Selection, new Event());

			vm.setDuration(nk.durationInMillis);
			vm.setSelectedPaletteByIndex(nk.palIndex);

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
			
			if (nk.frameSeqName != null)
				vm.setSelectedFrameSeq(vm.scenes.get(nk.frameSeqName));

			vm.setSelectedFrame(nk.frameIndex);
			vm.setDetectionMaskActive(nk.withMask);
			vm.setSelectedHashIndex(nk.hashIndex);
			if( nk.withMask ) {
				vm.dmd.setMask(vm.masks.get(nk.maskNumber));
				vm.setSelectedMaskNumber(nk.maskNumber);
			}
			
			hashCmdHandler.updateHashes(vm.dmd.getFrame());
			
			vm.setMaskSpinnerEnabled(nk.withMask);
			
			if( vm.selectedRecording!=null )
				vm.saveTimeCode = (int) vm.selectedRecording.getTimeCode(nk.frameIndex);
		} else {
			vm.setSelectedKeyFrame(null);
		}
		vm.setBtnSetHashEnabled(nk != null);
		vm.setDeleteKeyFrameEnabled(nk != null);
		vm.setSetKeyFramePalEnabled(nk != null && SwitchMode.PALETTE.equals(nk.switchMode));
		vm.setFetchDurationEnabled(nk != null);
	}
	
	private static final int FRAME_RATE = 40;
	
	public void onFetchDuration() {
		vm.setDuration(vm.lastTimeCode - vm.saveTimeCode);
	}
	
	public void onDurationChanged(int o, int n) {
		if (vm.selectedKeyFrame != null) {
			log.debug("setting duration for {}", vm.selectedKeyFrame.name);
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
				}
			}
		}
	}

}
