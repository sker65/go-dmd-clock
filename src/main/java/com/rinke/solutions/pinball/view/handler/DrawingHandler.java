package com.rinke.solutions.pinball.view.handler;

import java.util.Optional;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.CmdDispatcher.Command;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.widget.DrawTool;
import com.rinke.solutions.pinball.widget.PasteTool;

@Bean
public class DrawingHandler extends ViewHandler {

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
		if( maskIsVisible ) {
			if( vm.selectedRecording != null ) {
				// set the current global mask
				Mask mask = model.masks.get(vm.maskNumber);
				vm.dmd.setMask(mask.data);
				vm.setMaskLocked(mask.locked);
				vm.setDrawingEnabled(true);
				vm.setNumberOfPlanes(1);
			}
			Optional<CompiledAnimation> scene = model.getScene(vm.selectedScene);
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
			// TODO commit dmd changes
			vm.setDrawingEnabled(false);
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
