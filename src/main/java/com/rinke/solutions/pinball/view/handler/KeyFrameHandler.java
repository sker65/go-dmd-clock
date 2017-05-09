package com.rinke.solutions.pinball.view.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.TypedLabel;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
public class KeyFrameHandler extends ViewHandler {
	
	private int saveTimeCode;
	@Autowired
	private MessageUtil messageUtil;

	public KeyFrameHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm, m, d);
		populate();
	}
	
	public void onDeleteKeyFrame( TypedLabel item ) {
		PalMapping p = search( item );
		if( p != null ) {
			model.palMappings.remove(p);
			populate();
		}
		checkReleaseMask();
	}
	
	private void populate() {
		vm.keyframes.clear();
		for(PalMapping p : model.palMappings) {
			vm.keyframes.add( new TypedLabel(p.switchMode.name(), p.name));
		}
	}

	public void onFetchDuration() {
		vm.setDuration( vm.timecode - saveTimeCode );
		PalMapping selectedPalMapping = search( vm.selectedKeyFrame);
		if( selectedPalMapping != null ) {
			selectedPalMapping.durationInMillis = vm.duration;
			selectedPalMapping.durationInFrames = (int) selectedPalMapping.durationInMillis / 30;
		}
	}
	
	private PalMapping search( TypedLabel sel) {
		for(PalMapping p : model.palMappings) {
			if( p.name.equals(sel.label)) return p;
		}
		return null;
	}
	
	public void onSortKeyFrames() {
		Collections.sort(model.palMappings, new Comparator<PalMapping>() {
			@Override
			public int compare(PalMapping o1, PalMapping o2) {
				return o1.name.compareTo(o2.name);
			}
		});
		populate();
	}
	
	/**
	 * 
	 * @param switchMode
	 */
	public void onAddFrameSeq(SwitchMode switchMode) {
		// retrieve switch mode from selected scene edit mode!!
		if (vm.selectedFrameSeq != null) {
			if (vm.selectedHashIndex != -1) {
				CompiledAnimation ani = model.scenes.get(vm.selectedFrameSeq.label);
				//  add index, add ref to framesSeq
				if( !switchMode.equals(SwitchMode.PALETTE)) {
					switch(ani.getEditMode()) {
					case REPLACE:
						switchMode = SwitchMode.REPLACE;
						break;
					case COLMASK:
						switchMode = SwitchMode.ADD;
						break;
					case FOLLOW:
						switchMode = SwitchMode.FOLLOW;
						break;
					default:
						switchMode = SwitchMode.EVENT;
					}
				}
				PalMapping palMapping = new PalMapping(0, "KeyFrame " + ani.getDesc());
				palMapping.setDigest(vm.hashes.get(vm.selectedHashIndex));
				palMapping.palIndex = vm.selectedPalette.index;
				palMapping.frameSeqName = ani.getDesc();
				palMapping.animationName = vm.selectedRecording.label;
				palMapping.switchMode = switchMode;
				palMapping.frameIndex = vm.actFrame;
				if (vm.maskVisible) {
					palMapping.withMask = true;
					palMapping.maskNumber = vm.maskNumber;
					model.masks.get(vm.maskNumber).locked = true;
					//onMaskChecked(true);
				}
				if (!checkForDuplicateKeyFrames(palMapping)) {
					model.palMappings.add(palMapping);
					populate();
				} else {
					messageUtil.warn("duplicate hash", "There is already another Keyframe that uses the same hash");
				}
			} else {
				messageUtil.warn("no hash selected", "in order to create a key frame mapping, you must select a hash");
			}
		} else {
			messageUtil.warn("no scene selected", "in order to create a key frame mapping, you must select a scene");
		}	
	}

	public void onAddKeyFrame(SwitchMode switchMode) {
		PalMapping palMapping = new PalMapping(vm.selectedPalette.index, "KeyFrame " + (model.palMappings.size() + 1));
		if (vm.selectedHashIndex != -1) {
			palMapping.setDigest(vm.hashes.get(vm.selectedHashIndex));
		}
		palMapping.animationName = vm.selectedRecording.label;
		palMapping.frameIndex = vm.actFrame;
		if( switchMode.equals(SwitchMode.EVENT)) {
			palMapping.durationInMillis = (vm.eventHigh<<8) + vm.eventLow;
		}
		palMapping.switchMode = switchMode;
		if (vm.maskVisible) {
			palMapping.withMask = true;
			palMapping.maskNumber = vm.maskNumber;
			model.masks.get(vm.maskNumber).locked = true;
		}

		if (!checkForDuplicateKeyFrames(palMapping)) {
			model.palMappings.add(palMapping);
			populate();
			
			saveTimeCode = vm.timecode;
		} else {
			messageUtil.warn("Hash is already used", "The selected hash is already used by another key frame");
		}
	}

	boolean checkForDuplicateKeyFrames(PalMapping palMapping) {
		for (PalMapping p : model.palMappings) {
			if (Arrays.equals(p.crc32, palMapping.crc32))
				return true;
		}
		return false;
	}
	
	/**
	 * checks all pal mappings and releases masks if not used anymore
	 */
	private void checkReleaseMask() {
		HashSet<Integer> useMasks = new HashSet<>();
		for (PalMapping p : model.palMappings) {
			if (p.withMask) {
				useMasks.add(p.maskNumber);
			}
		}
		for (int i = 0; i < model.masks.size(); i++) {
			model.masks.get(i).locked = useMasks.contains(i);
			if( i == vm.maskNumber ) vm.setMaskLocked(useMasks.contains(i));
		}	
	}

}
