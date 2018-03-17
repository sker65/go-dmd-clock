package com.rinke.solutions.pinball.view.handler;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class MaskHandler extends AbstractCommandHandler implements ViewBindingHandler {

	@Autowired
	HashCmdHandler hashCmdHandler;
	@Autowired
	DrawCmdHandler drawCmdHandler;

	public MaskHandler(ViewModel vm) {
		super(vm);
	}
	
	/**
	 * button callback when mask checkbox is clicked.
	 * @param useMask
	 */
	public void onDetectionMaskActiveChanged(boolean old, boolean useMask) {
		// either we use masks with follow hash mode on scenes
		// or we use global masks on recordings
		if (useMask) {
			vm.setPaletteToolPlanes(1);
			vm.setMask(getCurrentMask());
			// if edit mode requires use mask of the scene, turn off global masks
			vm.setUseGlobalMask(!vm.selectedEditMode.useMask);
		} else {
			vm.setPaletteToolPlanes(vm.dmd.getNumberOfPlanes());
			// direkt gebunden
			//v.dmdWidget.setShowMask(false);
			
			if( vm.useGlobalMask ) { // commit edited global mask
				Mask mask = vm.masks.get(vm.selectedMask);
				if(vm.dmd.getFrame().mask!=null) {
					mask.commit(vm.dmd.getFrame().mask);
					vm.setDirty(true);
				}
			}
			vm.setMask(null);
			vm.dmd.removeMask();
			vm.setUseGlobalMask(false);
		}
		vm.setBtnInvertEnabled(useMask);
		
		hashCmdHandler.updateHashes(vm.dmd.getFrame());
		
		//bound to mask active vm.setDmdDirty(true);
		drawCmdHandler.setDrawMaskByEditMode(vm.selectedEditMode);
		updateDrawingEnabled();
	}

	private boolean isEditable(java.util.List<Animation> a) {
		if (a != null) {
			return a.size() == 1 && a.get(0).isMutable();
		}
		return false;
	}

	/**
	 * deletes the 2 additional color masking planes.
	 * depending on draw mask (presence of a mask) this is plane 2,3 or 3,4
	 */
	 public void onDeleteColMask() {
		vm.dmd.addUndoBuffer();
		vm.dmd.fill(vm.detectionMaskActive?(byte)0xFF:0);
		vm.setDmdDirty(true);
	}

	public void onInvertMask() {
		if( vm.dmd.hasMask() ) { // TODO check why this is called sometimes without mask
			vm.dmd.addUndoBuffer();
			byte[] data = vm.dmd.getFrame().mask.data;
			for( int i = 0; i < data.length; i++) {
				data[i] = (byte) ~data[i];
			}
			vm.dmd.setMask(data);
		}
	}

	public void onSelectedMaskChanged(int actMaskNumber, int newMaskNumber) {
		boolean hasChanged = false;
		if(newMaskNumber != actMaskNumber ) {
			log.info("mask number changed {} -> {}", actMaskNumber, newMaskNumber);
			actMaskNumber = newMaskNumber;
			hasChanged = true;
		}
		if (vm.useGlobalMask && hasChanged) {
			while( vm.masks.size()-1 < newMaskNumber ) {
				vm.masks.add(new Mask(vm.dmdSize.planeSize));
			}
			vm.setMask(vm.masks.get(newMaskNumber));
			// was v.dmdWidget.setMask(maskToUse);
			
			updateDrawingEnabled();
		}
	}

	private void updateDrawingEnabled() {
		vm.setDrawingEnabled((vm.useGlobalMask && !vm.masks.get(vm.selectedMask).locked) || !vm.animationIsPlaying && isEditable(vm.playingAnis));
	}

	/**
	 * get current mask, either from scene or from on of the global masks
	 * @return
	 */
	public Mask getCurrentMask() {
		Mask maskToUse = null; 
		if( vm.selectedEditMode.useMask) {
			// create mask from actual scene
			if( vm.selectedScene!=null) maskToUse = vm.selectedScene.getCurrentMask();
		} else {
			// use one of the project masks
			maskToUse = vm.masks.get(vm.selectedMask);
		}
		return maskToUse;
	}


	
}
