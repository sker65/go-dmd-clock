package com.rinke.solutions.pinball;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Observable;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.eclipse.swt.widgets.Scale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.AniEvent.Type;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.io.GifSequenceWriter;

/**
 * handles the sequence of animations and clock
 * @author sr
 */
public class AnimationHandler extends Observable implements Runnable{
    
    private static Logger LOG = LoggerFactory.getLogger(AnimationHandler.class); 

	private List<Animation> anis;
	private int index = 0; // index of the current animation
	private DMDClock clock;
	private boolean clockActive;
	private int clockCycles;
	private EventHandler eventHandler;
	private DMD dmd;
	private volatile boolean stop = false;
	private Scale scale;
	private boolean showClock = true;
	//private int transitionFrame= 0;
	private int lastRenderedFrame = -1;

	private byte[] mask;

	private boolean forceRerender = false;
	
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
	        LOG.error("unexpected exception caught: {}", e.getMessage(), e);
	    }
	}
	
	public void runInner() {
		if( clockActive ) {
			if( clockCycles == 0 ) dmd.clear();
			clock.renderTime(dmd,false);//,true,5,5);
			if( !stop && clockCycles++ > 20 && !anis.isEmpty() ) {
				clockActive = false;
				clockCycles = 0;
				clock.restart();
				//not used transitionFrame=0;
			}
			if( scale.isDisposed() ) return;
			eventHandler.notifyAni(new AniEvent(Type.CLOCK));
		} else {
			if( anis.isEmpty() ) {
				clockActive = true;
			} else {
				
				Animation ani = anis.get(index); 
				if( scale.isDisposed() ) return;
				scale.setMinimum(ani.start);
				scale.setMaximum(ani.end);
				scale.setIncrement(ani.skip);
				
				if( !forceRerender  && stop && ani.actFrame == lastRenderedFrame ) return;
				//System.out.println("rendering: "+ani.actFrame);
				
				forceRerender = false;
				dmd.clear();
				if( ani.addClock() ) {
				    if( ani.isClockSmall())
				        clock.renderTime(dmd, ani.isClockSmall(), ani.getClockXOffset(),ani.getClockYOffset(),false);
				    else
				        clock.renderTime(dmd,false);
				}
				Frame res = ani.render(dmd,stop);
                scale.setSelection(ani.actFrame);
                eventHandler.notifyAni(
                        new AniEvent(Type.ANI, ani.actFrame, ani, res.getHashes(mask), 
                                res.timecode, res.delay, res.planes.size() ));
                
                lastRenderedFrame = ani.actFrame;
                
                if( res.planes.size()==3 ) { // there is a mask
                    if( ani.getClockFrom()>ani.getTransitionFrom())
                        dmd.writeNotAnd(res.planes.get(2).plane); // mask out clock
                    DMD tmp = new DMD(dmd.getWidth(), dmd.getHeight());
                    tmp.writeOr(res);
                    tmp.writeAnd(res.planes.get(2).plane);       // mask out ani
                    dmd.writeOr(tmp.getFrame()); // merge
                } else {
                    // now if clock was rendered, use font mask to mask out digits in animation
                    if( ani.addClock() ) {
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
					if( showClock) clockActive = true;
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
		anis.get(index).prev();
		run();
	}

	public void next() {
		anis.get(index).next();
		run();
	}

	public void setScale(Scale scale) {
		this.scale = scale;
	}

	public void setPos(int pos) {
		if( !anis.isEmpty() ) {
		    anis.get(index).setPos(pos);
	        run();
		}
	}

	public void setAnimations(java.util.List<Animation> anis2) {
		this.anis = anis2;
		clockActive=false;
		index = 0;
		if( !anis.isEmpty() ) {
		    anis.get(index).restart();
		    run();
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
		this.clockActive = clockActive;
	}

	public void setMask(byte[] mask) {
		this.mask = mask;
		if( stop ) {
			forceRerender = true;
			run();
		}
	}

}


