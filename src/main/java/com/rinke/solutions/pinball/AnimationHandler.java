package com.rinke.solutions.pinball;

import java.util.List;

import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.pinball.renderer.FrameSet;

/**
 * handles the sequence of animations and clock
 * @author sr
 */
public class AnimationHandler implements Runnable {

	private List<Animation> anis;
	private int index = 0;
	private DMDClock clock;
	private boolean clockActive;
	private int clockCycles;
	private Canvas canvas;
	private Shell shell;
	private DMD dmd;
	private volatile boolean stop = false;
	private Scale scale;
	
	public AnimationHandler(List<Animation> anis, DMDClock clock, DMD dmd, Canvas canvas) {
		this.anis = anis;
		this.clock = clock;
		this.canvas = canvas;
		this.dmd = dmd;
	}

	public void run() {
		if( clockActive ) {
			if( clockCycles == 0 ) dmd.clear();
			clock.renderTime(dmd);//,true,5,5);
			if( !stop && clockCycles++ > 20 ) {
				clockActive = false;
				clockCycles = 0;
				clock.restart();
			}
			if( shell.isDisposed() ) return;
			shell.setText("clock");
		} else {
			Animation ani = anis.get(index); 
			
			scale.setMinimum(ani.start);
			scale.setMaximum(ani.end);
			scale.setIncrement(ani.skip);
			
			shell.setText(ani.getDesc()+" : "+ani.act);
			
			dmd.clear();
			if( ani.addClock() ) {
				clock.renderTime(dmd,ani.isClockSmall(), ani.getClockXOffset(),ani.getClockYOffset());
			}
			
			FrameSet frameSet = ani.render(dmd,stop);
			scale.setSelection(ani.act);
			dmd.writeOr(frameSet);
	
			if( ani.hasEnded() ) {
				ani.restart();
				clockActive = true;
				index++;
				if( index >= anis.size()) {
					index = 0;
				}
			}
		}
		canvas.redraw();
	}

	public int getRefreshDelay() {
		int d = clockActive?100:anis.get(index).getRefreshDelay();
		return d;
	}

	/** 
	 * sets the shell used to set ani text
	 * @param shell2
	 */
	public void setShell(Shell shell2) {
		this.shell = shell2;
	}
	
	public void start() {
		stop = false;
	}
	
	public void stop() {
		stop = true;
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

}


