package com.rinke.solutions.pinball.view.handler;

import com.rinke.solutions.beans.Autowired;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.model.PalMapping;
import org.eclipse.swt.widgets.Display; 
import org.eclipse.swt.dnd.Clipboard; 
import org.eclipse.swt.dnd.TextTransfer; 
import org.eclipse.swt.dnd.Transfer; 

import java.util.ArrayList;
import java.util.List;

@Bean
public class MaskHandler extends AbstractCommandHandler implements ViewBindingHandler {

	@Autowired 
	MessageUtil messageUtil;
	@Autowired
	HashCmdHandler hashCmdHandler;
	@Autowired
	DrawCmdHandler drawCmdHandler;
	@Autowired
	AnimationHandler animationHandler;

	public MaskHandler(ViewModel vm) {
		super(vm);
	}
	
	public void onDetectionMaskActiveChanged(boolean old, boolean n) {
		if(vm.selectedScene!=null && n && vm.selectedScene.actFrame == vm.selectedFrame) {
			vm.selectedScene.commitDMDchanges(vm.dmd); 
			vm.setDirty(vm.dirty|vm.selectedScene.isDirty());
		}
		if(vm.selectedEditMode.enableLayerMask)
			vm.setLayerMaskEnabled(!n);
		updateMaskChange(n, true);
		vm.setShowMask(n);
		animationHandler.forceRerender();
	}
	
	public void onLayerMaskActiveChanged(boolean old, boolean n) {
		if(vm.selectedScene!=null && n) {
			vm.selectedScene.commitDMDchanges(vm.dmd); 
			vm.setDirty(vm.dirty|vm.selectedScene.isDirty());
		}
		vm.setDetectionMaskEnabled(!n);
		vm.setMaskSpinnerEnabled(!n);
		updateMaskChange(n, false);
		vm.setShowMask(n);
	}
	
	public void commitMaskIfNeeded(boolean preferDetectionMask) {
		Mask mask = animationHandler.getCurrentMask(preferDetectionMask);
		if( mask != null && !mask.locked && vm.dmd.getFrame().mask!=null && vm.dmd.canUndo() ) {
			mask.commit(vm.dmd.getFrame().mask.data);
			vm.setDirty(true);
		}
	}

	void updateMaskChange(boolean n, boolean preferDetectionMask) {
		Mask mask = animationHandler.getCurrentMask(preferDetectionMask); 
		if (!n) {
			commitMaskIfNeeded(preferDetectionMask);
			if (!vm.detectionMaskActive && !vm.layerMaskActive)
				mask = null;
		}
		vm.dmd.setMask(mask);
		vm.setDeleteColMaskEnabled(vm.detectionMaskActive || vm.layerMaskActive);
		vm.setBtnInvertEnabled(vm.detectionMaskActive || vm.layerMaskActive);
		//animationHandler.forceRerender();
		vm.setDmdDirty(true);
		hashCmdHandler.updateHashes(vm.dmd.getFrame());
		if (n) {
			vm.setPaletteToolPlanes(1);
		} else {
			vm.setPaletteToolPlanes(vm.dmd.getNumberOfPlanes());
		}
		
		//bound to mask active vm.setDmdDirty(true);
		drawCmdHandler.setDrawMaskByEditMode(vm.selectedEditMode);
		updateDrawingEnabled();
	}

	public void onSelectedMaskNumberChanged(int oldMaskNumber, int newMaskNumber) {
		if (newMaskNumber == -1) {
			commitMaskIfNeeded(true);
		} else {
			if (vm.selectedEditMode.enableDetectionMask) {
				Mask maskToUse = null;
				if( vm.selectedEditMode.haveSceneDetectionMasks ){
					maskToUse = vm.selectedScene.getMaskWithSize(vm.selectedMaskNumber,vm.srcDmdSize.planeSize); 
				} else {
					// fill up global masks
					while( vm.masks.size()-1 < newMaskNumber ) {
						vm.masks.add(new Mask(vm.dmdSize.planeSize));
					}
					maskToUse = vm.masks.get(newMaskNumber);
				}
				vm.dmd.setMask(maskToUse);
				updateDrawingEnabled();
			}
			if( vm.selectedEditMode.enableLayerMask ) {
				if( vm.selectedScene != null) {
					vm.dmd.setMask(vm.selectedScene.getMaskWithSize(newMaskNumber, vm.srcDmdSize.planeSize));
					updateDrawingEnabled();
				}
			}
			animationHandler.forceRerender();
			vm.setDmdDirty(true);
		}
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
		EditMode m = vm.selectedEditMode;
		if( m.haveSceneDetectionMasks ||  m.haveLocalMask ) {
			if(vm.selectedScene != null && vm.dmd.getFrame().mask.locked) {
				int res = messageUtil.warn(0, "Warning",
						"Unlock Mask", 
						"If you unlock this mask all hashes using this mask need to be set again !",
						new String[]{"", "OK", "Cancel"},2);
				if( res == 1 ) {
					vm.dmd.getFrame().mask.locked = false;
					vm.setDmdDirty(true);
					updateDrawingEnabled();
				}
				
			} else { 				
				vm.dmd.addUndoBuffer();
				vm.dmd.fill((vm.layerMaskActive||vm.detectionMaskActive)?(byte)0xFF:0);
			}
			vm.setDmdDirty(true);
		} else {
			if (!(vm.dmd.getFrame().mask.locked)) {
				vm.dmd.addUndoBuffer();
				vm.dmd.fill((vm.layerMaskActive||vm.detectionMaskActive)?(byte)0xFF:0);
				vm.setDmdDirty(true);
			} else {
				if (vm.selectedScene == null) {
					//Show which keyframes use the mask here
					List<String> res = new ArrayList<>();
					for( PalMapping pm : vm.keyframes.values()) {
						if( pm.maskNumber == vm.selectedMaskNumber ) {
							res.add(" "+pm.name);
						}
					}
					// TODO local detection masks on a scene will never be unlocked
					if (res.size() == 0) {
						vm.masks.get(vm.selectedMaskNumber).locked = false;
						vm.dmd.getFrame().mask.locked = false;
						vm.setDmdDirty(true);
					} else {
						messageUtil.warn("Mask cannot be deleted", "It is used by the following Keyframes:\n"+res);
						Clipboard clipboard=new Clipboard(Display.getCurrent());
						TextTransfer transfer=TextTransfer.getInstance();
						clipboard.setContents(new Object[]{res.toString()},new Transfer[]{transfer});
						clipboard.dispose();
					}
				}
			}
		}
		
	}

	public void onInvertMask() {
		if (vm.detectionMaskActive) {
			EditMode m = vm.selectedEditMode;
			if( m.haveSceneDetectionMasks ) {
				if(vm.selectedScene != null && !vm.selectedScene.getMask(vm.selectedMaskNumber).locked) {
					vm.dmd.invertMask();
				} else {
					messageUtil.warn("Mask Locked", 
							"This mask is already used and cannot be modified");
				}
			} else {
				if (!(vm.dmd.getFrame().mask.locked)) {
					vm.dmd.invertMask();
				} else {
					messageUtil.warn("Mask Locked", 
							"This mask is already used and cannot be modified");
				}
			}
		} else if (vm.layerMaskActive) {
			vm.dmd.invertMask();
		}
	}

	public void updateDrawingEnabled() {
		EditMode m = vm.selectedEditMode;
		boolean drawing = false;
		if( !vm.animationIsPlaying ) {
			// falls mask drawing enabled UND einer der mask drawing buttons gesetzt ist UND
			// die betreffende maske (global oder scenen maske) nicht gelockt ist
			drawing = m.enableMaskDrawing && (vm.detectionMaskActive || vm.layerMaskActive );
			if( m.enableDetectionMask ) {
				if( m.haveSceneDetectionMasks || m.haveLocalMask) {
					if(vm.selectedScene != null && vm.selectedScene.getMask(vm.selectedMaskNumber).locked) 
						drawing = false;	
				} else {
					if( vm.masks.get(vm.selectedMaskNumber).locked) 
						drawing = false;
				}
			}
			if( m.enableLayerMask && vm.layerMaskActive ) drawing = true; // layer masks always
			// falls kein masks drawing, dann nur bei editierbaren scenen
			if( !(vm.detectionMaskActive || vm.layerMaskActive ) ) {
				drawing = isEditable(vm.playingAnis) ;
			}
		}
		vm.setDrawingEnabled( drawing );
	}
	
}
