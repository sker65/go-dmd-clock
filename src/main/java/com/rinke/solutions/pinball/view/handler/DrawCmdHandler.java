package com.rinke.solutions.pinball.view.handler;

import java.util.Arrays;
import java.util.Observable;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.MaskDmdObserver;
import com.rinke.solutions.pinball.ObserverManager;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.animation.AniEvent.Type;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.widget.DMDWidget.Rect;

@Bean
@Slf4j
public class DrawCmdHandler extends AbstractCommandHandler implements EventHandler, ViewBindingHandler {

	MaskDmdObserver maskDmdObserver;
	DMD dmd;
	@Autowired MessageUtil messageUtil;
	@Autowired RecordingsCmdHandler recordingsCmdHandler;
	@Autowired HashCmdHandler hashCmdHandler;
	@Autowired LivePreviewHandler livePreviewHandler;
	private int numberOfHashes = 4;
	@Autowired AnimationHandler animationHandler;

	public DrawCmdHandler(ViewModel vm, DMD s) {
		super(vm);
		maskDmdObserver = new MaskDmdObserver();
		maskDmdObserver.setDmd(s);
		this.dmd = s;
		ObserverManager.bind(maskDmdObserver, e -> vm.setUndoEnabled(e), () -> maskDmdObserver.canUndo());
		ObserverManager.bind(maskDmdObserver, e -> vm.setRedoEnabled(e), () -> maskDmdObserver.canRedo());
		maskDmdObserver.addObserver((dmd,o)->updateHashes(dmd));
	}
	
	@Override
	public void notifyAni(AniEvent evt) {
		switch (evt.evtType) {
		case ANI:
			//vm.setSelectedFrame(evt.ani.actFrame);
			vm.setTimecode( evt.frame.timecode);
			vm.setDelay(evt.frame.delay);
			vm.setNumberOfPlanes( evt.frame.planes.size());

			hashCmdHandler.updateHashes(evt.frame);
			
			vm.lastTimeCode = evt.frame.timecode;
			
			if (vm.livePreviewActive && evt.frame != null) {
				livePreviewHandler.sendFrame(evt.frame);
			}
			break;
		case CLOCK:
			vm.setSelectedFrame(0);;
			vm.setTimecode(0);
			String[] l = Arrays.copyOf(vm.hashLbl, numberOfHashes );
			for (int j = 0; j < 4; j++)
				l[j++]=""; // clear hashes
			vm.setHashLbl(l);
			break;
		case CLEAR:
			for (int j = 0; j < 4; j++)
				vm.hashLbl[j++]=""; // clear hashes
			vm.setHashLbl(vm.hashLbl);
			if (vm.livePreviewActive) {
				livePreviewHandler.sendFrame(new Frame());
			}
			break;
		case FRAMECHANGE:
			hashCmdHandler.updateHashes(evt.frame);
			break;
		}
		vm.setDmdDirty(true);
	}

	
	public void onSelectedEditModeChanged(EditMode old, EditMode mode) {
		if( vm.selectedScene != null ) {
			CompiledAnimation animation = vm.selectedScene;
			if( animation.isDirty() && !animation.getEditMode().equals(vm.selectedEditMode)) {
				int res = messageUtil.warn(SWT.ICON_WARNING | SWT.YES | SWT.NO, 
						"Changing edit mode", "you are about to change edit mode, while scene was already modified. Really change?");
				if( res == SWT.NO ) {
					vm.setSelectedEditMode(animation.getEditMode());
				}
			}
			if(vm.selectedEditMode.useMask) {
				animation.ensureMask();
			} 
			vm.setMaskEnabled(vm.selectedEditMode.useMask);
			recordingsCmdHandler.setEnableHashButtons(vm.selectedEditMode.useMask);
			animation.setEditMode(vm.selectedEditMode);
		}
		setDrawMaskByEditMode(vm.selectedEditMode);
	}
	
	/**
	 * called to remove rubber band selection
	 */
	public void onRemoveSelection() {
		if( vm.drawingEnabled ) {
			vm.setSelection(null);
		}
	}

	/**
	 * called to mark all
	 */
	public void onSelectAll() {
		if( vm.drawingEnabled ) {
			vm.setSelection(new Rect(0, 0, dmd.getWidth(), dmd.getHeight()));
		}
	}

	/**
	 * sets the draw mask in the dmd according to the selected EditMode
	 * @param mode
	 */
	public void setDrawMaskByEditMode(EditMode mode) {
		if( vm.maskActive ) {
			// only draw on mask
			// TODO mask drawing and plane drawing with mask should be controlled seperately
			vm.dmd.setDrawMask( 0b00000001);
		} else {
			vm.setDeleteColMaskEnabled(mode.useColorMasking);
			vm.dmd.setDrawMask(mode.useColorMasking ? 0b11111000 : 0xFFFF);
		}
	}

	public void onSelectedSceneChanged(CompiledAnimation o, CompiledAnimation n) {
		vm.setBtnDelFrameEnabled(n!=null && n.frames.size()>1);
	}
	
	void updateHashes(Observable o) {
		onUpdateHashes();
	}

	public void onUpdateHashes() {
		notifyAni(new AniEvent(Type.FRAMECHANGE, null, dmd.getFrame()));
	}
	
	public void onRedo() {
		maskDmdObserver.redo();
		vm.setDmdDirty(true);
	}

	public void onUndo() {
		maskDmdObserver.undo();
		vm.setDmdDirty(true);
	}
	
	public void onAddFrame() {
		CompiledAnimation ani = vm.selectedScene;
		log.info("adding frame at {}", ani.actFrame);
		ani.addFrame(ani.actFrame, new Frame(ani.frames.get(ani.actFrame)));
		animationHandler.updateScale(ani);
		vm.setDmdDirty(true);
		vm.setDirty(true);
	}

	public void onRemoveFrame() {
		CompiledAnimation ani = vm.selectedScene;
		if( ani.frames.size()>1 ) {
			ani.removeFrame(ani.actFrame);
			if( ani.actFrame >= ani.end ) {
				animationHandler.setPos(ani.end);
				vm.setSelectedFrame(ani.end);
			}
			animationHandler.updateScale(ani);
			vm.setDmdDirty(true);
			vm.setDirty(true);
		}
		vm.setBtnDelFrameEnabled(ani.frames.size()>1);
	}



	
}
