package com.rinke.solutions.pinball;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.pinball.AniEvent.Type;
import com.rinke.solutions.pinball.renderer.FrameSet;
import com.rinke.solutions.pinball.renderer.GifSequenceWriter;

/**
 * handles the sequence of animations and clock
 * @author sr
 */
public class AnimationHandler implements Runnable {

	private List<Animation> anis;
	private int index = 0; // index of the current animation
	private DMDClock clock;
	private boolean clockActive;
	private int clockCycles;
	private Canvas canvas;
	private EventHandler eventHandler;
	private DMD dmd;
	private volatile boolean stop = false;
	private Scale scale;
	private boolean export;
	private GifSequenceWriter gifWriter;
	private boolean showClock = true;
	private int transitionFrame= 0;
	
	public AnimationHandler(List<Animation> anis, DMDClock clock, DMD dmd, Canvas canvas, boolean export) {
		this.anis = anis;
		this.clock = clock;
		this.canvas = canvas;
		this.dmd = dmd;
		this.export = export;
		if( export ) {
			ImageOutputStream outputStream;
			try {
				outputStream = new FileImageOutputStream(new File("export.gif"));
				gifWriter = new GifSequenceWriter(outputStream, BufferedImage.TYPE_INT_ARGB, 1000, false);
			} catch ( IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void run() {
		if( clockActive ) {
			if( clockCycles == 0 ) dmd.clear();
			clock.renderTime(dmd);//,true,5,5);
			if( !stop && clockCycles++ > 20 && !anis.isEmpty() ) {
				clockActive = false;
				clockCycles = 0;
				clock.restart();
				transitionFrame=0;
			}
			if( scale.isDisposed() ) return;
			eventHandler.notifyAni(new AniEvent(Type.CLOCK, 0, null));
		} else {
			if( anis.isEmpty() ) {
				clockActive = true;
			} else {
				
				Animation ani = anis.get(index); 
				if( scale.isDisposed() ) return;
				scale.setMinimum(ani.start);
				scale.setMaximum(ani.end);
				scale.setIncrement(ani.skip);
				
				eventHandler.notifyAni(new AniEvent(Type.ANI, ani.actFrame, ani));
				
				dmd.clear();
				if( ani.addClock() ) {
				    if( ani.isClockSmall())
				        clock.renderTime(dmd,ani.isClockSmall(), ani.getClockXOffset(),ani.getClockYOffset());
				    else
				        clock.renderTime(dmd);
				}
				List<FrameSet> res = ani.render(dmd,stop);
                scale.setSelection(ani.actFrame);

                if( res.size()>1 ) { // there is a mask
                    dmd.writeNotAnd(res.get(1));
                    DMD tmp = new DMD(dmd.getWidth(), dmd.getHeight());
                    tmp.writeOr(res.get(0));
                    tmp.writeAnd(res.get(1));
                    dmd.writeOr(tmp.getFrameSet());
                } else {
                    dmd.writeOr(res.get(0)); // die Animation wird drüber geodert
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
		if( export ) {
			try {
				gifWriter.writeToSequence(dmd.draw(), getRefreshDelay());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		canvas.redraw();
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
	public void setLabelHandler(EventHandler shell2) {
		this.eventHandler = shell2;
	}
	
	public void start() {
		stop = false;
	}
	
	public void stop() {
		stop = true;
		try {
			if( gifWriter != null ) gifWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		export = false;
	}

	public void prev() {
		anis.get(index).prev();
		run();
		canvas.redraw();
	}

	public void next() {
		anis.get(index).next();
		run();
		canvas.redraw();
	}

	public void setScale(Scale scale) {
		this.scale = scale;
	}

	public void setPos(int pos) {
		anis.get(index).setPos(pos);
		run();
		canvas.redraw();
	}

	public void setAnimations(java.util.List<Animation> anis2) {
		this.anis = anis2;
		clockActive=false;
		index = 0;
		if( !anis.isEmpty() ) {
		    anis.get(index).restart();
		}
	}

	public boolean isShowClock() {
		return showClock;
	}

	public void setShowClock(boolean showClock) {
		this.showClock = showClock;
	}

}


