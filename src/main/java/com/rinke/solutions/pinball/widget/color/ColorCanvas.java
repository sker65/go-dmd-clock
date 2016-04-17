package com.rinke.solutions.pinball.widget.color;

import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.rinke.solutions.pinball.widget.ResourceManagedCanvas;

public class ColorCanvas extends ResourceManagedCanvas {

	private ColorPicker cp;

	public ColorCanvas(Composite parent, int style, ColorPicker cp) {
		super(parent, style);
		this.cp = cp;
		if( cp != null ) this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if( cp != null ) cp.setRGB(rgb.red, rgb.green, rgb.blue);
			}
		});
	}

	Display display = Display.getCurrent();

	private RGB rgb = null;
	
	@Override
	protected void paintWidget(PaintEvent e) {
		GC g = e.gc;
		Color color = rgb!=null ? resourceManager.createColor(ColorDescriptor.createFrom(rgb)):null;
		Rectangle r = getBounds();
		g.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		g.drawRectangle(0,0,r.width-1,r.height-1);
		if( color != null ) {
			g.setBackground(color);
			g.fillRectangle(1, 1, r.width-2, r.height-2);
		} else {
			g.drawLine(0, 0, r.width-2, r.height-2);
			g.drawLine(0, r.height-2, r.width-2, 0);
		}
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		return new Point(wHint, hHint);
	}

	public RGB getRgb() {
		return rgb;
	}

	public void setRgb(RGB rgb) {
		this.rgb = rgb;
		redraw();
	}

	public void setRGB(int r, int g, int b) {
		setRgb(new RGB(r,g,b));
	}

}
