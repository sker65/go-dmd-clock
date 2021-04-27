package com.rinke.solutions.pinball.view.handler;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class AnimationControlHandler extends AbstractCommandHandler implements ViewBindingHandler {

	@Autowired AnimationHandler animationHandler;
	@Autowired MaskHandler maskHandler;
	
	public AnimationControlHandler(ViewModel vm) {
		super(vm);
	}
	
	// could depend on order, MAYBE the vm does not reflect the latest state !??
	public void onSelectedRecordingChanged(Animation o, Animation n) {
		if( n != null) {
			update(vm.minFrame, vm.selectedFrame, vm.maxFrame, vm.animationIsPlaying);
		}
	}

	public void onSelectedSceneChanged(Animation o, Animation n) {
		if( n != null) {
			update(vm.minFrame, vm.selectedFrame, vm.maxFrame, vm.animationIsPlaying);
		}
	}
	
	public void onSelectedFrameChanged(int o, int n) {
		commitChanges();
		animationHandler.setPos(n);
		update(vm.minFrame, n, vm.maxFrame, vm.animationIsPlaying);
		if( vm.selectedScene != null && (vm.selectedEditMode.haveLocalMask || vm.selectedEditMode.haveSceneDetectionMasks || vm.selectedEditMode.pullFrameDataFromAssociatedRecording)) {
			if (vm.layerMaskActive)
				maskHandler.updateMaskChange(false, false);
			else
				maskHandler.updateMaskChange(false, true);			
			vm.setHashVal(HashCmdHandler.getPrintableHashes(vm.selectedScene.frames.get(vm.selectedScene.actFrame).crc32));
		} else {
			vm.setHashVal("-");
		}
	}

	private void update(int minFrame, int actFrame, int maxFrame, boolean isPlaying) {
		if( isPlaying ) {
			vm.setBtnPrevEnabled(false);
			vm.setBtnNextEnabled(false);
		} else {
			vm.setBtnPrevEnabled(minFrame<actFrame);
			vm.setBtnNextEnabled(actFrame<maxFrame);
		}
	}

	public void onAnimationIsPlayingChanged(boolean o, boolean n) {
		update(vm.minFrame, vm.selectedFrame, vm.maxFrame, n);
	}
	
	public void onStartStop(boolean isPlaying) {
		if( isPlaying ) {
			log.info("Stopping animation playback");
			animationHandler.stop();
			vm.setAnimationIsPlaying(false);
		} else {
			log.info("Starting animation playback");
			if( vm.selectedScene != null ){
				vm.selectedScene.commitDMDchanges(vm.dmd); 
				vm.setDirty(vm.dirty|vm.selectedScene.isDirty());
			}
			animationHandler.start();
			//vm.setNextTimerExec(animationHandler.getRefreshDelay());
			
			vm.setSelection(null);
			vm.setAnimationIsPlaying(true);
		}
	}
	
	void commitChanges() {
		if(vm.selectedScene!=null) {
			vm.selectedScene.commitDMDchanges(vm.dmd); 
			vm.setDirty(vm.dirty|vm.selectedScene.isDirty());
		}
		maskHandler.commitMaskIfNeeded(vm.detectionMaskActive);
	}

	public void onPrevFrame() {
		vm.linkedFrameOffset=0;
		if(vm.minFrame < vm.selectedFrame) {
		vm.setSelectedFrame(vm.selectedFrame-vm.frameIncrement);
		vm.setSelection(null);
		}
	}
	
	public void onNextFrame() {
		vm.linkedFrameOffset=0;
		if((vm.selectedFrame + vm.frameIncrement ) <= vm.maxFrame) {
		vm.setSelectedFrame(vm.selectedFrame+vm.frameIncrement);
		vm.setSelection(null);
		}
	}
	
	public void onPreviewPrevFrame() {
		if( vm.selectedEditMode.pullFrameDataFromAssociatedRecording ) {
			vm.linkedFrameOffset--;
			animationHandler.forceRerender();
		} else if(vm.minFrame < vm.selectedFrame) {
			vm.setSelectedFrame(vm.selectedFrame-vm.frameIncrement);
			vm.setSelection(null);
		}
	}
	
	public void onPreviewNextFrame() {
		if( vm.selectedEditMode.pullFrameDataFromAssociatedRecording ) {
			vm.linkedFrameOffset++;
			animationHandler.forceRerender();
		} else if((vm.selectedFrame + vm.frameIncrement ) <= vm.maxFrame) {
			vm.setSelectedFrame(vm.selectedFrame+vm.frameIncrement);
			vm.setSelection(null);
		}
	}
	
	public void onCopyAndMoveToNextFrame() {
		if (vm.selectedScene != null) {
			CompiledAnimation ani = vm.selectedScene;
			if( !ani.hasEnded() ) {
				onNextFrame();
				ani.frames.get(ani.actFrame-1).copyToWithMask(vm.dmd.getFrame(), vm.dmd.getDrawMask());
				vm.setDmdDirty(true);
			}
		}
	}
	
	public void onCopyAndMoveToPrevFrame() {
		if (vm.selectedScene != null) {
			onPrevFrame();
			CompiledAnimation ani = vm.selectedScene;
			if( ani.getActFrame() >= ani.getStart() ) {
				ani.frames.get(ani.actFrame+1).copyToWithMask(vm.dmd.getFrame(), vm.dmd.getDrawMask());
				vm.setDmdDirty(true);
			}
		}
	}

	public void onUpdateDelay() {
		CompiledAnimation ani = vm.selectedScene;
		if( ani != null && vm.selectedFrame < ani.frames.size() ) {
			log.debug("Setting delay of frame {} to {}", vm.selectedFrame, vm.delay);
			ani.frames.get(vm.selectedFrame).delay = vm.delay;
		}
	}
	
	public void onUpdateKeyframe(boolean keyFrame) {
		CompiledAnimation ani = vm.selectedScene;
		if( ani != null && vm.selectedFrame < ani.frames.size() ) {
			log.debug("Setting KeyFrame Prop of frame {} to {}", vm.selectedFrame, keyFrame);
			ani.frames.get(vm.selectedFrame).keyFrame = keyFrame;
		}
	}
		

	public void setAnimationHandler(AnimationHandler animationHandler) {
		this.animationHandler = animationHandler;
	}
	
}
