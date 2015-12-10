package com.rinke.solutions.pinball.widget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.widget.PaletteTool.ColorIndexChangedListerner;

/*
 * notes:
 * - must create a tmp buffer on each event to support tool drawing like lasso
 * - must support undo redo somehow
 * - can only be active if animation is stopped.
 * 
 */

public abstract class DrawTool implements ColorIndexChangedListerner {
	
	protected int x1 = -1;
	protected int y1 = -1; // where mouse goes down
	protected int pressedButton;
	protected DMD dmd;
	protected int actualColor = 0;
	
	public DrawTool(int actualColor) {
		super();
		this.actualColor = actualColor;
	}

	public void handleMouse(Event e, int x, int y) {
        switch (e.type) {
        case SWT.MouseDown:
        	pressedButton = e.button;
        	x1 = x;
        	y1 = y;
        	mouseDown(x, y);
            break;
        case SWT.MouseUp:
        	pressedButton = 0;
        	mouseUp(x, y);
        	x1 = -1;
        	y1 = -1;
            break;
        case SWT.MouseMove:
        	mouseMove(x, y);
            break;
        default:
            break;
        }

	}
	
	public abstract void mouseMove(int x, int y);

	public abstract void mouseUp(int x, int y);
	
	public abstract void mouseDown(int x, int y);

	public void setDMD(DMD dmd) {
		this.dmd = dmd;
	}

	public int getActualColor() {
		return actualColor;
	}

	public void setActualColor(int actualColor) {
		this.actualColor = actualColor;
	}

}
