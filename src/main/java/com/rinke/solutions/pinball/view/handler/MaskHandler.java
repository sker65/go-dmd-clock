package com.rinke.solutions.pinball.view.handler;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
public class MaskHandler extends AbstractCommandHandler implements ViewBindingHandler {

	@Autowired
	HashCmdHandler hashCmdHandler;
	@Autowired
	DrawCmdHandler drawCmdHandler;
	@Autowired
	AnimationHandler animationHandler;

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
		Mask mask = animationHandler.getCurrentMask();
		if( mask != null && !mask.locked && vm.dmd.getFrame().mask!=null && vm.dmd.canUndo() ) {
			mask.commit(vm.dmd.getFrame().mask);
			vm.setDirty(true);
		}
	}

	public void onMaskActiveChanged(boolean o, boolean n) {
		// either we use masks with follow hash mode on scenes
		// or we use global masks on recordings
		if (n) {
			vm.setPaletteToolPlanes(1);
			vm.setSelectedMask(animationHandler.getCurrentMask());
		} else {
			vm.setPaletteToolPlanes(vm.dmd.getNumberOfPlanes());
			commitMaskIfNeeded();
			vm.setSelectedMask(null);
			vm.dmd.removeMask();
		}
		vm.setBtnInvertEnabled(n);
		animationHandler.forceRerender();
		vm.setDmdDirty(true);
		hashCmdHandler.updateHashes(vm.dmd.getFrame());
		
		//bound to mask active vm.setDmdDirty(true);
		drawCmdHandler.setDrawMaskByEditMode(vm.selectedEditMode);
		updateDrawingEnabled();
	}

	public void onSelectedMaskNumberChanged(int old, int newMaskNumber) {
		if (vm.selectedEditMode.enableDetectionMask) {
			while( vm.masks.size()-1 < newMaskNumber ) {
				vm.masks.add(new Mask(vm.dmdSize.planeSize));
			}
			vm.setSelectedMask(vm.masks.get(newMaskNumber));
			updateDrawingEnabled();
		}
		if( vm.selectedEditMode.enableLayerMask ) {
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
			// falls mask drawing enabled UND einer der mask drawing buttons gesetzt ist UND
			// die betreffende maske (global oder scenen maske) nicht gelockt ist
			drawing = m.enableMaskDrawing && (vm.detectionMaskActive || vm.layerMaskActive );
			if( m.enableDetectionMask ) {
				if( m.haveSceneDetectionMasks ) {
					if(vm.selectedScene != null && vm.selectedScene.getMask(vm.selectedMaskNumber).locked) 
						drawing = false;	
				} else {
					if( vm.masks.get(vm.selectedMaskNumber).locked) 
						drawing = false;
				}
			}
			// falls kein masks drawing, dann nur bei editierbaren scenen
			if( !(vm.detectionMaskActive || vm.layerMaskActive ) ) {
				drawing = isEditable(vm.playingAnis) ;
			}
		}
		vm.setDrawingEnabled( drawing );
	}
	
}
