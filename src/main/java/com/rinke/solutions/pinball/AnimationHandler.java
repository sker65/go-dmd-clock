package com.rinke.solutions.pinball;

import java.util.Arrays;
import java.util.List;
import java.util.Observable;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.AniEvent.Type;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.FrameScaler;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.animation.RawAnimation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.view.model.ViewModel;

/**
 * handles the sequence of animations and clock
 * @author sr
 */
@Slf4j
@Bean
public class AnimationHandler implements Runnable {

	private List<Animation> anis;
	private int index; // index of the current animation
	private final DMDClock clock;
	private boolean clockActive;
	private int clockCycles;
	private EventHandler eventHandler;
	private final DMD dmd;
	private volatile boolean stop = true;
	private boolean showClock = true;
	private int lastRenderedFrame = -1;

	private boolean forceRerender;
	@Value(key=Config.GODMD_ENABLED_PROP_KEY)
	private boolean enableClock;
	private ViewModel vm;

	public AnimationHandler( ViewModel vm, DMDClock clock, DMD dmd) {
		this.vm = vm;
		this.anis = vm.playingAnis;
		this.clock = clock;
		this.dmd = dmd;
	}

	public void run() {
	    try {
	        runInner();
	    } catch( Exception e) {
	        GlobalExceptionHandler.getInstance().setException(e);
	        anis.clear();
	        eventHandler.notifyAni(new AniEvent(Type.CLEAR));
	        log.error("unexpected exception caught: {}", e.getMessage(), e);
	    }
	}
	
	public void startClock() {
		if( enableClock ) {
			setClockActive(true);
			clockCycles = 0;
			clock.restart();
			run();
		}
	}
	
	public void runInner() {
		if( clockActive ) {
			if( clockCycles == 0 ) dmd.clear();
			clock.renderTime(dmd,false);//,true,5,5);
			if( !stop && clockCycles++ > 20 && !anis.isEmpty() ) {
				setClockActive(false);
				clockCycles = 0;
				clock.restart();
				//not used transitionFrame=0;
			}
			//if( scale.isDisposed() ) return;
			eventHandler.notifyAni(new AniEvent(Type.CLOCK));
		} else {
			if( anis==null || anis.isEmpty() ) {
				setClockActive(true);
			} else {
				
				//if( scale.isDisposed() ) return;

				Animation ani = anis.get(index); 
				Animation linkedAnimation = null; 
				updateScale(ani);
				
				if( !forceRerender  && stop && ani.actFrame == lastRenderedFrame ) return;
				
				forceRerender = false;
				dmd.clear();
				if( enableClock && ani.addClock() ) {
					ani.setClockWasAdded(true);
				    if( ani.isClockSmall())
				        clock.renderTime(dmd, ani.isClockSmall(), ani.getClockXOffset(),ani.getClockYOffset(),false);
				    else
				        clock.renderTime(dmd,false);
				}
				log.debug("rendering ani: {}@{}", ani.getDesc(), ani.getActFrame());
				int actFrame = ani.getActFrame();
        		vm.setSelectedLinkFrame(actFrame);
				
				// now there is more logic here:
				Frame res = ani.render(dmd,stop);
				Frame previewRes = null;
				EditMode mode = vm.selectedEditMode;
				if( ani instanceof CompiledAnimation ) {
					CompiledAnimation cani = (CompiledAnimation)ani;
					RecordingLink link = cani.getRecordingLink();
					if((mode.pullFrameDataFromAssociatedRecording && link != null) || (cani.frames.get(0).planes.size()==24 && link != null) ) {
						// calc offset
						int frameNo = 0;
						if (vm.selectedScene != null && vm.selectedScene.getActualFrame().frameLink != null) {
            				linkedAnimation = vm.recordings.get(vm.selectedScene.getActualFrame().frameLink.recordingName);
            				frameNo = vm.selectedScene.getActualFrame().frameLink.frame + vm.linkedFrameOffset;
						} else if (vm.selectedScene != null && vm.selectedScene.getPreviousFrame().frameLink != null) {
							linkedAnimation = vm.recordings.get(vm.selectedScene.getPreviousFrame().frameLink.recordingName);
            				frameNo = vm.selectedScene.getPreviousFrame().frameLink.frame + 1 + vm.linkedFrameOffset;
						} else if (vm.selectedScene != null && vm.selectedScene.getNextFrame().frameLink != null) {
							linkedAnimation = vm.recordings.get(vm.selectedScene.getNextFrame().frameLink.recordingName);
            				frameNo = vm.selectedScene.getNextFrame().frameLink.frame - 1 + vm.linkedFrameOffset;
            			} else {
            				linkedAnimation = vm.recordings.get(link.associatedRecordingName);
    						frameNo =  link.startFrame + actFrame + vm.linkedFrameOffset;
            			}
						if (linkedAnimation != null) {
							if (frameNo < 0) {
								frameNo = 0;
								vm.setLinkedFrameOffset(vm.linkedFrameOffset+1);
								}
							if (frameNo >= linkedAnimation.end) {
								frameNo = linkedAnimation.end;
								vm.setLinkedFrameOffset(vm.linkedFrameOffset-1);
								}
							
	                		if(vm.previewDMD == null) {
		    					DMD previewDMD = new DMD(linkedAnimation.width,linkedAnimation.height);
		    					vm.setPreviewDMD(previewDMD);
		    				}
	                		

	                		vm.setSelectedLinkFrame(frameNo);
	                		previewRes = linkedAnimation.render(frameNo,vm.previewDMD,stop);
	                		
		                	if( linkedAnimation instanceof RawAnimation && vm.previewDMD != null) {
		                		RawAnimation rani = (RawAnimation)linkedAnimation;
		                		previewRes = rani.renderSubframes(vm.previewDMD, frameNo);
		                	}
	
	                        previewRes.setMask(getCurrentMask(vm.detectionMaskActive));
	                        
	                        if(cani.frames.get(0).planes.size()==24) {
	                        	vm.setPreviewDmdPalette(vm.previewPalettes.get(5));
	                        }
	                        
	                        vm.setLinkVal(linkedAnimation.getDesc()+":"+frameNo);
							vm.selectedLinkRecordingName = linkedAnimation.getDesc();
		                	vm.previewDMD.setFrame(previewRes);
	                	}
					}
				}
				
                if(ani instanceof RawAnimation) {
            		if(vm.previewDMD == null) {
    					DMD previewDMD = new DMD(ani.width,ani.height);
    					vm.setPreviewDMD(previewDMD);
    				}
                	RawAnimation rani = (RawAnimation)ani;       	
                	Frame tmp = rani.renderSubframes(vm.previewDMD, actFrame);
                    tmp.setMask(getCurrentMask(vm.detectionMaskActive));
                	vm.previewDMD.setFrame(tmp);
                }
                
                lastRenderedFrame = ani.actFrame;
                
                // only dmd playback nothing goDMD like
                if( false && res.hasMask() ) { // there is a mask
                    if( ani.getClockFrom()>ani.getTransitionFrom())
                        dmd.writeNotAnd(res.planes.get(2).data); // mask out clock
                    DMD tmp = new DMD(dmd.getWidth(), dmd.getHeight());
                    tmp.writeOr(res);
                    tmp.writeAnd(res.planes.get(2).data);       // mask out ani
                    dmd.writeOr(tmp.getFrame()); // merge
                } else {
                    // now if clock was rendered, use font mask to mask out digits in animation
                    if( enableClock && ani.addClock() ) {
                        DMD tmp = new DMD(dmd.getWidth(), dmd.getHeight());
                        tmp.writeOr(res);
                        clock.renderTime(tmp,true); // mask out time
                        dmd.writeOr(tmp.getFrame());
                    } else {
                    	log.debug("writing to dmd: {}", dmd);
                    	if( linkedAnimation != null && vm.detectionMaskActive ) {
                    		ensureDmdSize(linkedAnimation);
                    		dmd.writeOr(previewRes);
                    	} else {
                    		ensureDmdSize(ani);
                    		dmd.writeOr(res);
                    	}
                        //dmd.dumpHistogram();
                    }
                }
                Frame f = dmd.getFrame();
                f.delay = res.delay;
                f.timecode = res.timecode;
                f.keyFrame = res.keyFrame;
                eventHandler.notifyAni(new AniEvent(Type.ANI, ani, f));

                if( ani.hasEnded() ) {
					if( !ani.isMutable() ){
//						ani.restart();
						if( showClock) setClockActive(true);
						index++;
						if( index >= anis.size()) {
							index = 0;
						}
					}
				}
			}
		}
	}
	
	public void ensureDmdSize(Animation ani) {
		if( dmd.getWidth() != ani.width || dmd.getHeight() != ani.height ) {
			dmd.setSize(ani.width, ani.height, true);
			vm.setDmdSize(DmdSize.fromWidthHeight(ani.width, ani.height));
		}
	}

	public void updateScale(Animation ani) {
		vm.setMinFrame(ani.start);
		vm.setMaxFrame(ani.end);
		vm.setFrameIncrement(ani.skip);
	}

	public int getRefreshDelay() {
		if( clockActive||anis.isEmpty() ) {
			return 100;
		}
		return anis.get(index).getRefreshDelay() / vm.playSpeed;
	}

	/** 
	 * sets the shell used to set ani text
	 * @param shell2
	 */
	public void setEventHandler(EventHandler handler) {
		this.eventHandler = handler;
	}
	
	public void start() {
		setStop(false);
		vm.setAnimationIsPlaying(true);	
		vm.setStartStopLabel("Stop");
	}
	
	public void setStop(boolean b) {
		this.stop = b;
	}

	public void stop() {
		vm.setAnimationIsPlaying(false);	
		vm.setStartStopLabel("Start");
		setStop(true);
	}

	public void prev() {
		if( index <0 || index >= anis.size() ) return;
		anis.get(index).prev();
		run();
	}

	public void next() {
		if( index <0 || index >= anis.size() ) return;
		anis.get(index).next();
		run();
	}

	public Mask getCurrentMask() {
		return getCurrentMask(false);
	}
	/**
	 * get current mask, either from scene or from on of the global masks
	 * @return
	 */
	public Mask getCurrentMask(boolean preferDetectionMask) {
		Mask maskToUse = null; 
		if(vm.selectedEditMode != null) {
			if( vm.selectedScene!=null) {
				if( vm.selectedEditMode.haveLocalMask ) {
					maskToUse = vm.selectedScene.getCurrentMask();
				}
				if( vm.selectedEditMode.haveSceneDetectionMasks && preferDetectionMask ){
					if (vm.selectedScene.getMask(vm.selectedMaskNumber).data.length != vm.srcDmdSize.planeSize) {
						for (int i = 0; i < vm.selectedScene.getMasks().size(); i++) {
							vm.selectedScene.getMask(i).data = Arrays.copyOfRange(vm.selectedScene.getMask(i).data,0,vm.srcDmdSize.planeSize);
						}
					}
					maskToUse = vm.selectedScene.getMaskWithSize(vm.selectedMaskNumber, vm.srcDmdSize.planeSize); 
				}
			}
			if( vm.selectedEditMode.enableDetectionMask && !vm.selectedEditMode.haveSceneDetectionMasks
					&& !vm.selectedEditMode.haveLocalMask) {
				// use one of the global masks
				if( preferDetectionMask ) maskToUse = vm.masks.get(vm.selectedMaskNumber);
			}
		}
		return maskToUse;
	}

	/**
	 * like setPos but use currentMask to populate DMD mask while animation
	 * @param pos
	 */
	public void setPos(int pos) {
		if( index <0 || index >= anis.size() ) return;
	    anis.get(index).setPos(pos);
	    forceRerender = true;
	    vm.dmd.setMask(getCurrentMask(vm.detectionMaskActive));
	    log.debug("setpos {} @ {} with mask {}", pos, anis.get(index).getDesc(), dmd.getFrame().mask);
        //run();
	}
	
	public void forceRerender() {
		forceRerender = true;
		run();
	}
	
	public java.util.List<Animation> getAnimations() {
		return anis;
	}

	public void setAnimations(java.util.List<Animation> anisToSet) {
		log.debug("setAnimations {}", anisToSet.stream().map(a->a.getDesc()));
		this.anis = anisToSet;
		setClockActive(false);
		index = 0;
		if( !anis.isEmpty() ) {
			forceRerender = true;
		    anis.get(index).restart();
		    //run();
		} else {
			startClock();
		}
	}

	public boolean isShowClock() {
		return showClock;
	}

	public void setShowClock(boolean showClock) {
		this.showClock = showClock;
	}

    public boolean isStopped() {
        return stop;
    }

	public boolean isClockActive() {
		return clockActive;
	}

	public void setClockActive(boolean clockActive) {
		this.clockActive = enableClock && clockActive;
	}

	public boolean hasAnimations() {
		return !anis.isEmpty();
	}

	public boolean isEnableClock() {
		return enableClock;
	}

	public void setEnableClock(boolean b) {
		this.enableClock = b;
	}

	public void setVm(ViewModel vm) {
		this.vm = vm;
	}

}


