package com.rinke.solutions.pinball.view.handler;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
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
	
	public void onDetectionMaskActiveChanged(boolean old, boolean useMask) {
		vm.setShowMask(useMask || vm.layerMaskActive );
		onMaskActiveChanged(old, useMask);
	}
	
	public void onLayerMaskActiveChanged(boolean old, boolean useMask) {
		vm.setShowMask(useMask || vm.detectionMaskActive );
		onMaskActiveChanged(old, useMask);
	}
	
	public void commitMaskIfNeeded() {
		Mask mask = getCurrentMask();
		if( mask != null && vm.dmd.getFrame().mask!=null && vm.dmd.canUndo() ) {
			mask.commit(vm.dmd.getFrame().mask);
			vm.setDirty(true);
		}
	}

	public void onMaskActiveChanged(boolean old, boolean useMask) {
		// either we use masks with follow hash mode on scenes
		// or we use global masks on recordings
		if (useMask) {
			vm.setPaletteToolPlanes(1);
			vm.setSelectedMask(getCurrentMask());
			// if edit mode requires use mask of the scene, turn off global masks
			vm.setUseGlobalMask(vm.selectedEditMode.useGlobalMask);
		} else {
			vm.setPaletteToolPlanes(vm.dmd.getNumberOfPlanes());
			commitMaskIfNeeded();
			vm.setSelectedMask(null);
			vm.dmd.removeMask();
		}
		vm.setBtnInvertEnabled(useMask);
		vm.setDmdDirty(true);
		hashCmdHandler.updateHashes(vm.dmd.getFrame());
		
		//bound to mask active vm.setDmdDirty(true);
		drawCmdHandler.setDrawMaskByEditMode(vm.selectedEditMode);
		updateDrawingEnabled();
	}

	public void onSelectedMaskNumberChanged(int old, int newMaskNumber) {
		if (vm.selectedEditMode.useGlobalMask) {
			while( vm.masks.size()-1 < newMaskNumber ) {
				vm.masks.add(new Mask(vm.dmdSize.planeSize));
			}
			vm.setSelectedMask(vm.masks.get(newMaskNumber));
			updateDrawingEnabled();
		}
		if( vm.selectedEditMode.useLayerMask ) {
			if( vm.selectedScene != null) {
				vm.setSelectedMask(vm.selectedScene.getMask(newMaskNumber));
				updateDrawingEnabled();
			}
		}
		vm.setDmdDirty(true);
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
		vm.dmd.fill((vm.layerMaskActive||vm.detectionMaskActive)?(byte)0xFF:0);
		vm.setDmdDirty(true);
	}

	public void onInvertMask() {
		vm.dmd.invertMask();
	}

	public void updateDrawingEnabled() {
		EditMode m = vm.selectedEditMode;
		boolean drawing = false;
		if( !vm.animationIsPlaying ) {
			if( m.enableMaskDrawing && (vm.detectionMaskActive || vm.layerMaskActive ) ) {
				if( m.useGlobalMask ) {
					drawing = !vm.masks.get(vm.selectedMaskNumber).locked;
				} else {
					drawing = true;
				}
			}
			if( !(vm.detectionMaskActive || vm.layerMaskActive ) ) {
				drawing = isEditable(vm.playingAnis) ;
			}
		}
		vm.setDrawingEnabled( drawing );
	}

	/**
	 * get current mask, either from scene or from on of the global masks
	 * @return
	 */
	public Mask getCurrentMask() {
		Mask maskToUse = null; 
		if( vm.selectedScene!=null) {
			if( vm.selectedEditMode.useLocalMask) {
				// create mask from actual scene
				maskToUse = vm.selectedScene.getCurrentMask();
			} else if( vm.selectedEditMode.useLayerMask ){
				maskToUse = vm.selectedScene.getMask(vm.selectedMaskNumber); 
			}
		}
		if( vm.selectedEditMode.useGlobalMask ) {
			// use one of the global masks
			maskToUse = vm.masks.get(vm.selectedMaskNumber);
		}
		return maskToUse;
	}


	
}
