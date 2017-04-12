package com.rinke.solutions.pinball.widget;

import java.util.Observable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.widget.PaletteTool.ColorChangedListerner;
import com.rinke.solutions.pinball.widget.PaletteTool.ColorIndexChangedListerner;

/*
 * notes:
 * - must create a tmp buffer on each event to support tool drawing like lasso
 * - must support undo redo somehow
 * - can only be active if animation is stopped.
 * 
 */

public abstract class DrawTool extends Observable implements ColorIndexChangedListerner {
	
	protected int x1 = -1;
	protected int y1 = -1; // where mouse goes down
	protected int pressedButton;
	protected DMD dmd;
	protected int actualColor = 0;
	
	public DrawTool(int actualColor) {
		super();
		this.actualColor = actualColor;
	}

	/**
	 * handles mouse
	 * @param e event
	 * @param x x coord
	 * @param y y coord
	 * @return true if redraw needed
	 */
	public boolean handleMouse(Event e, int x, int y) {
        switch (e.type) {
        case SWT.MouseDown:
        	pressedButton = e.button;
        	x1 = x;
        	y1 = y;
        	return mouseDown(x, y);
        case SWT.MouseUp:
        	pressedButton = 0;
        	boolean ret = mouseUp(x, y);
        	x1 = -1;
        	y1 = -1;
        	setChanged(); notifyObservers();
        	return ret;
        case SWT.MouseMove:
        	return mouseMove(x, y);
        default:
            break;
        }
        return false;
	}
	
	public boolean mouseMove(int x, int y) { return false; }

	public boolean mouseUp(int x, int y) { return false; }
	
	public boolean mouseDown(int x, int y)  { return false; }

	public void setDMD(DMD dmd) {
		this.dmd = dmd;
	}

	public int getActualColor() {
		return actualColor;
	}

	@Override
	public void indexChanged(int actualColor) {
		this.actualColor = actualColor;
	}
	
	public void paletteChanged(Palette p ) {
		
	}

}
