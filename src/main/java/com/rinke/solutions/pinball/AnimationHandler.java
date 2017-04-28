package com.rinke.solutions.pinball;

import java.util.List;
import java.util.Observable;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.widgets.Scale;

import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.AniEvent.Type;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.model.Frame;

/**
 * handles the sequence of animations and clock
 * @author sr
 */
@Slf4j
public class AnimationHandler extends Observable implements Runnable{

	private List<Animation> anis;
	private int index; // index of the current animation
	private final DMDClock clock;
	private boolean clockActive;
	private int clockCycles;
	private EventHandler eventHandler;
	private final DMD dmd;
	private volatile boolean stop = true;
	private Scale scale;
	private boolean showClock = true;
	private int lastRenderedFrame = -1;

	private boolean forceRerender;
	private boolean enableClock;
	
	public AnimationHandler(List<Animation> anis, DMDClock clock, DMD dmd) {
		this.anis = anis;
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
			if( scale.isDisposed() ) return;
			eventHandler.notifyAni(new AniEvent(Type.CLOCK));
		} else {
			if( anis==null || anis.isEmpty() ) {
				setClockActive(true);
			} else {
				
				if( scale.isDisposed() ) return;

				Animation ani = anis.get(index); 
				scale.setMinimum(ani.start);
				scale.setMaximum(ani.end);
				scale.setIncrement(ani.skip);
				
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
				Frame res = ani.render(dmd,stop);
                scale.setSelection(ani.actFrame);
                eventHandler.notifyAni(
                        new AniEvent(Type.ANI, ani, res ));
                
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
                        dmd.writeOr(res);
                    }
                }
		
				if( ani.hasEnded() ) {
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
	}
	
	public void setStop(boolean b) {
		this.stop = b;
		setChanged();
		notifyObservers();
	}

	public void stop() {
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

	public void setScale(Scale scale) {
		this.scale = scale;
	}

	public void setPos(int pos) {
		if( index <0 || index >= anis.size() ) return;
	    anis.get(index).setPos(pos);
        run();
	}
	
	public java.util.List<Animation> getAnimations() {
		return anis;
	}

	public void setAnimations(java.util.List<Animation> anisToSet) {
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
		setChanged();
		notifyObservers();
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

}


