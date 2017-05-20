package com.rinke.solutions.pinball.view.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
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
	
	@Autowired
	private PaletteHandler paletteHandler;

	public KeyFrameHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm, m, d);
		model.palMappings.addObserver((o,a)->populate());
	}
	
	public void onSelectedKeyFrameChanged(PalMapping ov, PalMapping nv) {
		Optional<PalMapping> p = Optional.ofNullable(nv);
		if( p.isPresent() ) {
			PalMapping m = p.get();
			// TODO depends on switch mode
			CompiledAnimation cani = model.scenes.get(m.animationName);
			vm.setSelectedFrameSeq(cani);
			EditMode editMode = cani.getEditMode();
			vm.setSelectedScene(cani);
			if( !EditMode.FOLLOW.equals(editMode) ) {
				vm.setSelectedMaskNumber(m.maskNumber);
			}
			// scene setzen reicht, palette switched automatisch
			// Palette palette = paletteHandler.getPaletteByIndex(m.palIndex);
			vm.setSelectedFrame(m.frameIndex);
		}
	}
	
	public void onDeleteKeyFrame( TypedLabel item ) {
		model.getPalMapping(item).ifPresent( p-> {
			model.palMappings.remove(p);
			populate();
		});
		checkReleaseMask();
	}
	
	public void populate() {
		vm.keyframes.clear();
		for( PalMapping p : model.palMappings) {
			vm.keyframes.add( new TypedLabel(p.switchMode.name(), p.name));
		}
	}

	public void onFetchDuration() {
		vm.setDuration( vm.timecode - saveTimeCode );
		Optional.ofNullable( vm.selectedKeyFrame).ifPresent(p->{
			p.durationInMillis = vm.duration;
			p.durationInFrames = (int) p.durationInMillis / 30;
		});
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
		Optional<CompiledAnimation> optScene = Optional.ofNullable(vm.selectedFrameSeq);
		if (optScene.isPresent() ) {
			if (vm.selectedHashIndex != -1) {
				//  add index, add ref to framesSeq
				if( !switchMode.equals(SwitchMode.PALETTE)) {
					switch( optScene.map(ani->ani.getEditMode()).orElse(EditMode.FIXED) ) {
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
				
				final SwitchMode sm = switchMode;		
				Optional<PalMapping> optPalMapping = optScene.map( s-> {
					PalMapping palMapping = new PalMapping(0, "KeyFrame " + s.getDesc());
					palMapping.setDigest(vm.hashes.get(vm.selectedHashIndex));
					palMapping.palIndex = vm.selectedPalette.index;
					palMapping.frameSeqName = s.getDesc();
					palMapping.animationName = vm.selectedRecording.getDesc();
					palMapping.switchMode = sm;
					palMapping.frameIndex = vm.selectedFrame;
					if (vm.maskVisible) {
						palMapping.withMask = true;
						palMapping.maskNumber = vm.selectedMaskNumber;
						model.masks.get(vm.selectedMaskNumber).locked = true;
						//onMaskChecked(true);
					}
					return palMapping;
				});
				
				optPalMapping.ifPresent(pal->{
					if (!checkForDuplicateKeyFrames(pal)) {
						model.palMappings.add(pal);
						populate();
					} else {
						messageUtil.warn("duplicate hash", "There is already another Keyframe that uses the same hash");
					}
				});
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
		palMapping.animationName = vm.selectedRecording.getDesc();
		palMapping.frameIndex = vm.selectedFrame;
		if( switchMode.equals(SwitchMode.EVENT)) {
			palMapping.durationInMillis = (vm.selectedEventHigh<<8) + vm.selectedEventLow;
		}
		palMapping.switchMode = switchMode;
		if (vm.maskVisible) {
			palMapping.withMask = true;
			palMapping.maskNumber = vm.selectedMaskNumber;
			model.masks.get(vm.selectedMaskNumber).locked = true;
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
			if( i == vm.selectedMaskNumber ) vm.setMaskLocked(useMasks.contains(i));
		}	
	}

}
