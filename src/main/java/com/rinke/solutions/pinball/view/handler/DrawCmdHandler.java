package com.rinke.solutions.pinball.view.handler;

import java.util.Arrays;
import java.util.Observable;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.Constants;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.Dispatcher;
import com.rinke.solutions.pinball.MaskDmdObserver;
import com.rinke.solutions.pinball.ObserverManager;
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
	@Autowired AnimationHandler animationHandler;
	@Autowired private Dispatcher dispatcher;
	
	private int numberOfHashes = 4;


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
			vm.setKeyFrame(evt.frame.keyFrame);

			hashCmdHandler.updateHashes(evt.frame);
			
			vm.setLastTimeCode( evt.frame.timecode );
			
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
			if(vm.previewDMD != null) {
				vm.previewDMD.setMask(vm.dmd.getFrame().mask);
			}
			break;
		}
		vm.setDmdDirty(true);
	}
	
	public void onSelectionChanged( Rect o, Rect n) {
		vm.setCutEnabled(true);
		vm.setCopyEnabled(true);
	}
	
	public void onDrawingEnabledChanged(boolean o, boolean n) {
		vm.setCopyToNextEnabled(n);
		vm.setCopyToPrevEnabled(n);
		vm.setToolSizeSpinnerEnabled(n);
		vm.setBtnDelFrameEnabled(n);
	}

	public void onSelectedFrameChanged(int ov, int nv) {
		vm.setCopyToNextEnabled(vm.drawingEnabled && nv<vm.maxFrame);
		vm.setCopyToPrevEnabled(vm.drawingEnabled && nv>vm.minFrame);
	}
	
	public void onSuggestedEditModeChanged(final EditMode old, final EditMode mode) {
		EditMode modeToSet = mode;
		if( vm.selectedScene != null ) {
			CompiledAnimation animation = vm.selectedScene;
			boolean setMode = true;
			if( animation.isDirty() && !animation.getEditMode().equals(mode)) {
				int res = messageUtil.warn(0, "Warning",
						"Changing edit mode", 
						"you are about to change edit mode to '"+mode.label+"', while scene was already modified.",
						new String[]{"", "Cancel", "Change Mode"},2);
				if( res == 1 ) {
					setMode = false;
					modeToSet = old;
				}
			}
			if( setMode ) {
				animation.setEditMode(mode);
				if(mode.haveLocalMask ) {
					animation.ensureMask();
				}
				vm.setSelectedEditMode(mode);
			} else {
				// reset suggested to selected (veto)
				dispatcher.asyncExec(()->vm.setSuggestedEditMode(old));
			}
			if( modeToSet != null ) {
				vm.setDetectionMaskEnabled(modeToSet.enableDetectionMask);
				vm.setLayerMaskEnabled(modeToSet.enableLayerMask);
				vm.setMaskSpinnerEnabled(modeToSet.enableDetectionMaskSpinner);
				vm.setSmartDrawEnabled(!modeToSet.enableColorMaskDrawing);
				recordingsCmdHandler.setEnableHashButtons(modeToSet.enableDetectionMask);
				vm.setBtnPreviewNextEnabled(modeToSet.pullFrameDataFromAssociatedRecording);
				vm.setBtnPreviewPrevEnabled(modeToSet.pullFrameDataFromAssociatedRecording);
			}
			
			// to force update on master detail
			vm.scenes.refresh();
			vm.setDirty(true);
		}
		setDrawMaskByEditMode(modeToSet);
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
		if( mode.enableMaskDrawing && (vm.detectionMaskActive || vm.layerMaskActive ) ) {
			// only draw on mask
			vm.setSmartDrawEnabled(false);
			vm.dmd.setDrawMask( 0b00000001);
		} else {
			//vm.setDeleteColMaskEnabled(mode.enableColorMaskDrawing);
			// either col mask drawing or normal drawing
			// bit 0 ist mask plane in dmd
			if( vm.selectedScene != null ) {
				vm.setSmartDrawEnabled(mode.enableColorMaskDrawing ? false : true);
			} else {
				vm.setSmartDrawEnabled(false);
			}
			
			if (vm.has4PlanesRecording)
				vm.dmd.setDrawMask(mode.enableColorMaskDrawing ? 0b11100000 : Constants.DEFAULT_DRAW_MASK);
			else
				vm.dmd.setDrawMask(mode.enableColorMaskDrawing ? 0b11111000 : Constants.DEFAULT_DRAW_MASK);
			
			if (vm.smartDrawEnabled) {
				if (vm.has4PlanesRecording)
					vm.dmd.setDrawMask(vm.smartDrawActive ? 0b11100000 : Constants.DEFAULT_DRAW_MASK);
				else
					vm.dmd.setDrawMask(vm.smartDrawActive ? 0b11111000 : Constants.DEFAULT_DRAW_MASK);
			}


		}
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
		if (vm.selectedScene != null) {
			CompiledAnimation ani = vm.selectedScene;
			log.info("adding frame at {}", ani.actFrame);
			ani.addFrame(ani.actFrame, new Frame(ani.frames.get(ani.actFrame)));
			animationHandler.updateScale(ani);
			vm.setDmdDirty(true);
			vm.setDirty(true);
			vm.setBtnNextEnabled(ani.actFrame<ani.end);
		}
	}

	public void onRemoveFrame() {
		if (vm.selectedScene != null) {
			CompiledAnimation ani = vm.selectedScene;
			if( ani.frames.size()>1 ) {
				ani.removeFrame(ani.actFrame);
				if( ani.actFrame >= ani.end ) {
					animationHandler.setPos(ani.end);
					vm.setSelectedFrame(ani.end);
				} else {
					animationHandler.setPos(ani.actFrame);
				}
				animationHandler.updateScale(ani);
				vm.setDirty(true);
			}
			animationHandler.forceRerender();
			vm.setBtnDelFrameEnabled(ani.frames.size()>1);
			vm.setBtnNextEnabled(ani.actFrame<ani.end);
		}
	}
	
	public void onSmartDrawActiveChanged(boolean old, boolean n) {
		if( vm.selectedScene != null ) {
			if (vm.has4PlanesRecording)
				vm.dmd.setDrawMask(n ? 0b11100000 : Constants.DEFAULT_DRAW_MASK);
			else
				vm.dmd.setDrawMask(n ? 0b11111000 : Constants.DEFAULT_DRAW_MASK);
		} 
	}

	
	public void onToolSizeChanged(int newToolSize) {
    //    log.info("tool size {}", newToolSize);
    } 
	
}
