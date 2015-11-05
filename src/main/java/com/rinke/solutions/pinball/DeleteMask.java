package com.rinke.solutions.pinball;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;

/**
 * class that holds a "delete mask". which is an area that is delete each time a
 * frame is rendered newly (prev / next)
 * 
 * @author sr
 *
 */
public class DeleteMask {

    int x;
    int y;
    int w;
    int h;
    
    public DeleteMask(int x, int y, int w, int h) {
        super();
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public void  drawMaskRects(PaintEvent ev) {
        Color color = new Color(ev.display, 0, 0xFF, 0);
        ev.gc.setForeground(color);
        ev.gc.drawRectangle(new Rectangle(x, y, w, h));
        color.dispose();
    }

    @Override
    public String toString() {
        return "DeleteMask [x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + "]";
    }

}
