package com.rinke.solutions.pinball;

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
	private Mask maskToPopulate;

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
				Frame res = ani.render(dmd,stop);
                if( ani instanceof RawAnimation && vm.previewDMD != null ) {
                	RawAnimation rani = (RawAnimation)ani;
                	rani.renderSubframes(vm.previewDMD, actFrame);
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
                        dmd.writeOr(res);
                        dmd.dumpHistogram();
                    }
                }
                if( maskToPopulate != null ) {
                	dmd.setMask(maskToPopulate.data);
                }
                Frame f = dmd.getFrame();
                f.delay = res.delay;
                f.timecode = res.timecode;
                eventHandler.notifyAni(new AniEvent(Type.ANI, ani, f));

                if( ani.hasEnded() ) {
					if( !ani.isMutable() ){
						ani.restart();
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

	public void updateScale(Animation ani) {
		vm.setMinFrame(ani.start);
		vm.setMaxFrame(ani.end);
		vm.setFrameIncrement(ani.skip);
	}

	public int getRefreshDelay() {
		if( clockActive||anis.isEmpty() ) {
			return 100;
		}
		return anis.get(index).getRefreshDelay();
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

	/**
	 * like setPos but use currentMask to populate DMD mask while animation
	 * @param pos
	 * @param currentMask
	 */
	public void setPos(int pos, Mask currentMask) {
		if( index <0 || index >= anis.size() ) return;
	    anis.get(index).setPos(pos);
	    forceRerender = true;
	    this.maskToPopulate = currentMask;
        run();
	}

	public void setPos(int pos) {
		if( index <0 || index >= anis.size() ) return;
	    anis.get(index).setPos(pos);
	    log.debug("setpos {} @ {}", pos, anis.get(index).getDesc());
	    forceRerender = true;
	    this.maskToPopulate = null;
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
		    run();
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


