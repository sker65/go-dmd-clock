package com.rinke.solutions.pinball.view.handler;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PlaneNumber;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.CmdDispatcher.Command;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.widget.DrawTool;
import com.rinke.solutions.pinball.widget.PasteTool;

@Bean
@Slf4j
public class DrawingHandler extends ViewHandler {

	@Autowired
	MessageUtil messageUtil;

	public DrawingHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm,m,d);
	}
	
	// observe palette changes
	// propaget to draw tool
	public void onSelectedPaletteChanged(Palette ov, Palette nv) {
		//vm.dmdWidget.setPalette ...
	}

	public void onDrawingEnabledChanged(boolean ov, boolean nv) {	
		vm.setCopyToNextEnabled(nv);
		vm.setCopyToPrevEnabled(nv);
		vm.setDeleteColMaskEnabled(nv);
		if( nv ) {
			vm.setSelectedEditMode(vm.playingAni.getEditMode());
			setupDraw(vm.playingAni);
		}
	}
	
	public void onInvertMask() {
		vm.dmd.addUndoBuffer();
		byte[] data = vm.dmd.getFrame().mask.data;
		for( int i = 0; i < data.length; i++) {
			data[i] = (byte) ~data[i];
		}
		vm.dmd.setMask(data);
		updateUndoRedo();
		vm.setFrameRedraw(vm.frameRedraw+1);
	}
	
	private void updateUndoRedo() {
		vm.setUndoEnabled(vm.dmd.canUndo());
		vm.setRedoEnabled(vm.dmd.canRedo());
	}
	
	public void onUndo() {
		vm.dmd.undo();
		updateUndoRedo();
		vm.setFrameRedraw(vm.frameRedraw+1);
	}
	
	public void onRedo() {
		vm.dmd.redo();
		updateUndoRedo();
		vm.setFrameRedraw(vm.frameRedraw+1);
	}
	
	public void onMaskVisibleChanged(boolean maskWasVisible, boolean maskIsVisible) {	
		vm.setMaskInvertEnabled(maskIsVisible);
		Optional<CompiledAnimation> scene = model.getScene(vm.selectedScene);
		if( maskIsVisible ) {
			if( vm.selectedRecording != null ) {
				// set the current global mask
				setupGlobalMask(vm.maskNumber);
				vm.setDrawingEnabled(true);
				vm.setNumberOfPlanes(1);
			}
			if( scene.isPresent() ) {
				vm.dmd.setMask(scene.get().getCurrentMask().data);
				vm.setDrawingEnabled(true);
				vm.setMaskLocked(false);
				vm.setNumberOfPlanes(1);
			}
			if( vm.drawingEnabled ) {
				vm.setDrawMask(0b00000001);
				vm.setFrameRedraw(vm.frameRedraw+1);
			}
		} else {
			if( vm.selectedEditMode.equals(EditMode.FIXED) ) {
				commitGlobalMask(vm.maskNumber);
			}
			// mask of means not in any case disable drawing of course
			if( scene.isPresent() ) {
				setupDraw(scene.get());
				setDrawMaskByEditMode(vm.selectedEditMode);
			} else {
				vm.setDrawingEnabled(false);
			}
		}
	}

	void setupGlobalMask(int maskNumber) {
		Mask mask = model.masks.get(maskNumber);
		vm.dmd.setMask(mask.data);
		vm.setMaskLocked(mask.locked);
	}
	
	public void onMaskNumberChanged( int ov, int nv) {
		if( vm.maskVisible && vm.selectedEditMode.equals(EditMode.FIXED)) {
			commitGlobalMask(ov);
			setupGlobalMask(nv);
		}
	}
	
	private void commitGlobalMask(int maskNumber) {
		log.debug("committing mask no: {}", maskNumber);
		Mask mask = model.masks.get(maskNumber);
		mask.commit(vm.dmd.getFrame().mask);
	}

	public void onSelectedEditModeChanged( EditMode ov, EditMode nv ) {
		Optional<CompiledAnimation> scene = model.getScene(vm.selectedScene);
		if( scene.isPresent()) {
			CompiledAnimation cani = scene.get();
			if( cani.isDirty() && !cani.getEditMode().equals(nv)) {
				int res = messageUtil.warn(SWT.ICON_WARNING | SWT.YES | SWT.NO, "Changing edit mode",
						"you are about to change edit mode, while scene was already modified. Really change?");
				if( res == SWT.NO ) {
					vm.setSelectedEditMode(cani.getEditMode());
				}
			}
			cani.setEditMode(nv);
			setupDraw(cani);
			setDrawMaskByEditMode(nv);
		}
	}

	void setupDraw(Animation cani) {
		switch( cani.getEditMode() ) {
		case FOLLOW:
			((CompiledAnimation) cani).ensureMask();
			vm.setMaskOnEnabled(true);
			vm.setHashButtonsEnabled(true);
			vm.setMaskSpinnerEnabled(false);
			break;
		case REPLACE:
		case COLMASK:
			vm.setMaskOnEnabled(false);
			vm.setHashButtonsEnabled(false);
			vm.setMaskSpinnerEnabled(false);
			break;
		case FIXED:
			vm.setMaskOnEnabled(true);
			vm.setHashButtonsEnabled(true);
			vm.setMaskSpinnerEnabled(true);
		}		
	}
	
	private void setDrawMaskByEditMode(EditMode mode) {
		if( vm.maskVisible) {
			// only draw on mask
			// TODO mask drawing and plane drawing with mask should be controlled seperately
			vm.dmd.setDrawMask( 0b00000001);
		} else {
			boolean drawWithMask = EditMode.COLMASK.equals(mode) || EditMode.FOLLOW.equals(mode);
			vm.setDeleteColMaskEnabled(drawWithMask);
			vm.dmd.setDrawMask(drawWithMask ? 0b11111000 : 0xFFFF);
		}
	}
	
	public void onSetDrawTool(String name) {
		vm.setDrawTool(name);
	}
	
	public void onActFrameChanged(int ov, int nv) {
		vm.setCopyToNextEnabled(vm.drawingEnabled && nv<vm.maxFrame);
		vm.setCopyToPrevEnabled(vm.drawingEnabled && nv>vm.minFrame);
	}

}
