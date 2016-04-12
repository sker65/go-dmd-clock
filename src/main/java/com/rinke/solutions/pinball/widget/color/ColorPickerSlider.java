/*
 * @(#)ColorPickerSliderUI.java
 *
 * $Date: 2014-03-13 04:15:48 -0400 (Thu, 13 Mar 2014) $
 *
 * Copyright (c) 2011 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * https://javagraphics.java.net/
 * 
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */
package com.rinke.solutions.pinball.widget.color;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.rinke.solutions.pinball.widget.ResourceManagedCanvas;

/**
 * This is a SliderUI designed specifically for the <code>ColorPicker</code>.
 * 
 */
public class ColorPickerSlider extends ResourceManagedCanvas implements MouseListener, MouseMoveListener {
	ColorPicker colorPicker;

	/** Half of the height of the arrow */
	int ARROW_HALF = 8;
	Display display = Display.getCurrent();

	int[] intArray = new int[Toolkit.getDefaultToolkit().getScreenSize().height];
	BufferedImage bi = new BufferedImage(1, intArray.length,
			BufferedImage.TYPE_INT_RGB);
	int lastMode = -1;

	private Rectangle thumbRect = new Rectangle(0, 0, 20, 15);
	private Rectangle trackRect = new Rectangle(0, 0, 20, 150);

	public ColorPickerSlider(Composite parent, ColorPicker cp, int style) {
		super(parent, style);
		colorPicker = cp;
		addMouseListener(this);
		addMouseMoveListener(this);
	}
	
	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		calculateThumbSize();
		calculateTrackRect();
	}

	public void paintThumb(GC g2) {
		int y = thumbRect.y + thumbRect.height / 2;
		int[] polygon = { 0, y - ARROW_HALF, ARROW_HALF+0, y, 0, y + ARROW_HALF, };

		g2.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		g2.fillPolygon(polygon);
		g2.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		g2.drawPolyline(polygon);
	}

	protected void calculateThumbSize() {
		thumbRect.height += 4;
		thumbRect.y -= 2;
	}

	protected void calculateTrackRect() {
//		ColorPickerPanel cp = colorPicker.getColorPanel();
//		int size = Math.min(ColorPickerPanel.MAX_SIZE,
//				Math.min(cp.getBounds().width,
//						 cp.getBounds().height));
//		
		int size = this.getBounds().height;
		int max = this.getBounds().height - ARROW_HALF * 2 - 2;
		if (size > max) {
			size = max;
		}
		trackRect.y = this.getBounds().height / 2 - size / 2;
		trackRect.height = size;
	}
	
	int getMode() {
		return colorPicker!=null?colorPicker.getMode():ColorPicker.HUE;
	}

	public synchronized void paintTrack(GC g, Display display) {
		int mode = getMode();
		if (mode == ColorPicker.HUE || mode == ColorPicker.BRI
				|| mode == ColorPicker.SAT) {
			float[] hsb = colorPicker!=null?colorPicker.getHSB():new float[]{0.3f,0.3f,0.3f};
			if (mode == ColorPicker.HUE) {
				for (int y = 0; y < trackRect.height; y++) {
					float hue = ((float) y) / ((float) trackRect.height);
					intArray[y] = HSBtoRGB(hue, 1f, 1f);
				}
			} else if (mode == ColorPicker.SAT) {
				for (int y = 0; y < trackRect.height; y++) {
					float sat = 1 - ((float) y) / ((float) trackRect.height);
					intArray[y] = HSBtoRGB(hsb[0], sat, hsb[2]);
				}
			} else {
				for (int y = 0; y < trackRect.height; y++) {
					float bri = 1 - ((float) y) / ((float) trackRect.height);
					intArray[y] = HSBtoRGB(hsb[0], hsb[1], bri);
				}
			}
		} else {
			int[] rgb = colorPicker.getRGB();
			if (mode == ColorPicker.RED) {
				for (int y = 0; y < trackRect.height; y++) {
					int red = 255 - (int) (y * 255 / trackRect.height + .49);
					intArray[y] = (red << 16) + (rgb[1] << 8) + (rgb[2]);
				}
			} else if (mode == ColorPicker.GREEN) {
				for (int y = 0; y < trackRect.height; y++) {
					int green = 255 - (int) (y * 255 / trackRect.height + .49);
					intArray[y] = (rgb[0] << 16) + (green << 8) + (rgb[2]);
				}
			} else if (mode == ColorPicker.BLUE) {
				for (int y = 0; y < trackRect.height; y++) {
					int blue = 255 - (int) (y * 255 / trackRect.height + .49);
					intArray[y] = (rgb[0] << 16) + (rgb[1] << 8) + (blue);
				}
			}
		}
		// if(slider.hasFocus()) {
		// PlafPaintUtils.paintFocus(g2,r,3);
		// }

		// http://www.java2s.com/Tutorial/Java/0280__SWT/ConvertbetweenSWTImageandAWTBufferedImage.htm

		bi.getRaster().setDataElements(0, 0, 1, trackRect.height, intArray);
		Image image = new Image(display, convertToSWT(bi));
		Pattern pattern = new Pattern(display,image);
		g.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		g.drawRectangle(5, trackRect.y, 14, trackRect.height-ARROW_HALF);
		g.setBackgroundPattern(pattern);
		g.fillRectangle(6, trackRect.y+1, 13, trackRect.height-ARROW_HALF-1);
		pattern.dispose();
		// PlafPaintUtils.drawBevel(g2, r);
	}

	ImageData convertToSWT(BufferedImage bufferedImage) {
		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) bufferedImage
					.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(),
					colorModel.getGreenMask(), colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(),
					bufferedImage.getHeight(), colorModel.getPixelSize(),
					palette);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					int pixel = palette.getPixel(new RGB(pixelArray[0],
							pixelArray[1], pixelArray[2]));
					data.setPixel(x, y, pixel);
				}
			}
			return data;
		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel) bufferedImage
					.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF,
						blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(),
					bufferedImage.getHeight(), colorModel.getPixelSize(),
					palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		}
		return null;
	}

	private int HSBtoRGB(float f, float g, float bri) {
		RGB rgb = new RGB(f*360f, g, bri);
		return (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
	}

	@Override
	protected void paintWidget(PaintEvent e) {
		paintTrack(e.gc, e.display);
		paintThumb(e.gc);
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		return new Point(wHint, hHint);
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
	}
	
	int but = 0;

	@Override
	public void mouseDown(MouseEvent e) {
		but = e.button;
	}
	
	private int value = 0;
	
	private void updateArrow(int y) {
		if( y >= ARROW_HALF && y < trackRect.height) {
			thumbRect.y = y - ARROW_HALF;
			int range = getRangeForMode(getMode());
			
			updateValue( range - thumbRect.y * range / (trackRect.height-ARROW_HALF-1) );
			redraw();
		}	
	}
	
	public void setValue(int v) {
		int range = getRangeForMode(getMode());
		if( v >= 0 && v <= range ) {
			thumbRect.y = (range-v) * (trackRect.height-ARROW_HALF-1) / range;
		}
		redraw();
	}

	private int getRangeForMode(int mode) {
		switch (mode) {
		case ColorPicker.HUE:
			return 360;
		case ColorPicker.SAT:
		case ColorPicker.BRI:
			return 100;

		default:
			return 255;
		}
	}

	public void updateValue(int i) {
		value = i;
		colorPicker.sliderValueChanged(i);
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if( e.button == 1 ) {
			updateArrow(e.y);
		}
		but = 0;
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if( but == 1 ) {
			updateArrow(e.y);
		}
	}

}
