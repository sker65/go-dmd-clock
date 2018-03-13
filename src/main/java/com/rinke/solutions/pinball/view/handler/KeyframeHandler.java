package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.cookie.BrowserCompatSpecFactory.SecurityLevel;
import org.eclipse.swt.widgets.Spinner;
import org.slf4j.Logger;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.Model;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class KeyframeHandler extends AbstractCommandHandler implements ViewBindingHandler {

	private Logger messageUtil;

	public KeyframeHandler(ViewModel vm) {
		super(vm);
	}

	public void onSelectedSpinnerDeviceIdChanged(int o, int n) {
		if( vm.selectedKeyFrame != null ) {
			vm.selectedKeyFrame.durationInMillis = vm.selectedSpinnerDeviceId << 8 +vm.selectedSpinnerEventId; 
		}
	}

	public void onSelectedSpinnerEventIdChanged(int o, int n) {
		if( vm.selectedKeyFrame != null ) {
			vm.selectedKeyFrame.durationInMillis = vm.selectedSpinnerDeviceId << 8 +vm.selectedSpinnerEventId; 
		}
	}

	public void onAddKeyFrame(SwitchMode switchMode) {
		PalMapping palMapping = new PalMapping(vm.selectedPalette.index, "KeyFrame " + (vm.keyframes.size() + 1));
		if (vm.selectedHashIndex != -1) {
			palMapping.setDigest(vm.hashes.get(vm.selectedHashIndex));
		}
		palMapping.animationName = vm.selectedRecording.getDesc();
		palMapping.frameIndex = vm.selectedRecording.actFrame;
		if( switchMode.equals(SwitchMode.EVENT)) {
			palMapping.durationInMillis = vm.selectedSpinnerDeviceId<<8 + vm.selectedSpinnerEventId;
		}
		palMapping.switchMode = switchMode;
		if (vm.useGlobalMask) {
			palMapping.withMask = true;
			palMapping.maskNumber = vm.selectedMask;
			vm.masks.get(vm.selectedMask).locked = true;
			vm.setMaskActive(true);
		}

		if (!checkForDuplicateKeyFrames(palMapping)) {
			vm.keyframes.put(palMapping.name,palMapping);
			vm.saveTimeCode = vm.lastTimeCode;
			vm.setDirty(true);
		} else {
			messageUtil.warn("Hash is already used", "The selected hash is already used by another key frame");
		}
	}

	public void onAddFrameSeq(EditMode editMode) {
		SwitchMode switchMode = SwitchMode.PALETTE;
		switch(editMode) {
			case COLMASK: switchMode = SwitchMode.ADD; break;
			case FOLLOW: switchMode = SwitchMode.FOLLOW; break;
			case REPLACE: switchMode = SwitchMode.REPLACE; break;
			default: break;
		}
		// retrieve switch mode from selected scene edit mode!!
		if (vm.selectedFrameSeq != null) {
			if (vm.selectedHashIndex != -1) {
				Animation ani = vm.selectedFrameSeq;
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
					case LAYEREDCOL:
						switchMode = SwitchMode.LAYEREDCOL;
						break;
					default:
						switchMode = SwitchMode.EVENT;
					}
				}
				PalMapping palMapping = new PalMapping(0, "KeyFrame " + ani.getDesc());
				palMapping.setDigest(vm.hashes.get(vm.selectedHashIndex));
				palMapping.palIndex = ani.getPalIndex();
				palMapping.frameSeqName = ani.getDesc();
				palMapping.animationName = vm.selectedRecording.getDesc();
				palMapping.switchMode = switchMode;
				palMapping.frameIndex = vm.selectedRecording.actFrame;
				if (vm.useGlobalMask) {
					palMapping.withMask = true;
					palMapping.maskNumber = vm.selectedMask;
					vm.masks.get(vm.selectedMask).locked = true;
					vm.setMaskActive(true);
				}
				if (!checkForDuplicateKeyFrames(palMapping)) {
					vm.keyframes.put(palMapping.name,palMapping);
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
	
	boolean checkForDuplicateKeyFrames(PalMapping palMapping) {
		for (PalMapping p : vm.keyframes.values()) {
			if (Arrays.equals(p.crc32, palMapping.crc32))
				return true;
		}
		return false;
	}
		

	public void onDeleteKeyframe() {
		if (vm.selectedKeyFrame != null) {
			vm.keyframes.remove(vm.selectedKeyFrame);
			checkReleaseMask();
		}
	}
	/**
	 * checks all pal mappings and releases masks if not used anymore
	 */
	void checkReleaseMask() {
		HashSet<Integer> useMasks = new HashSet<>();
		for (PalMapping p : vm.keyframes.values()) {
			if (p.withMask) {
				useMasks.add(p.maskNumber);
			}
		}
		for (int i = 0; i < vm.masks.size(); i++) {
			vm.masks.get(i).locked = useMasks.contains(i);
		}
		vm.setMaskActive(vm.useGlobalMask);
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
// to unselect
//			if (palMapping.equals(vm.selectedKeyFrame)) {
//				vm.setSelectedKeyFrame(null);
//				return;
//			}

			log.debug("selected new palMapping {}", nk);

			vm.setSelectedHashIndex(nk.hashIndex);

			// current firmware always checks with and w/o mask
			// btnMask.setSelection(selectedPalMapping.withMask);
			// btnMask.notifyListeners(SWT.Selection, new Event());

			vm.setDuration(vm.selectedKeyFrame.durationInMillis);
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
			
			if (vm.selectedKeyFrame.frameSeqName != null)
				vm.setSelectedFrameSeq(vm.scenes.get(nk.frameSeqName));

			vm.setSelectedFrame(nk.frameIndex);

			vm.setMaskActive(nk.withMask);
			if(nk.withMask) {
				vm.setMaskSpinnerEnabled(true);
				vm.setSelectedMask(nk.maskNumber);

				String[] lbls = Arrays.copyOf(vm.hashLbl, vm.hashLbl.length);
				String txt = lbls[vm.selectedHashIndex];
				if( !txt.startsWith("M")) lbls[vm.selectedHashIndex] = "M" + nk.maskNumber + " " + txt;
				vm.setHashLbl(lbls);
			}
			
			if( vm.selectedRecording!=null )
				vm.saveTimeCode = (int) vm.selectedRecording.getTimeCode(nk.frameIndex);
		} else {
			vm.setSelectedKeyFrame(null);
		}
		vm.setDeleteKeyFrameEnabled(nk != null);
		vm.setSetKeyFramePalEnabled(nk != null && SwitchMode.PALETTE.equals(nk.switchMode));
		vm.setFetchDurationEnabled(nk != null);
	}

	public void onSelectedFrameSeqChanged(Animation old, Animation ani) {
		vm.setBtnAddFrameSeqEnabled(ani != null && vm.selectedRecording!=null);
	}



}
