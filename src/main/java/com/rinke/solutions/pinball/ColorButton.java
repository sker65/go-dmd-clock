/*******************************************************************************
 * 
 * jKiwi -- A virtual makeover and hairstyler application
 * Copyright (c) 2007-2008 Dan Mihai Ile, Maria Joao Roque Barbado Leal
 * 
 * This file is part of jKiwi.
 * 
 * jKiwi is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jKiwi is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *******************************************************************************/

package com.rinke.solutions.pinball;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

/**
 * Instances of this class represents a button that
 * shows a background color and can be selected.
 * <p>
 * Note that although this class is a subclass of <code>Canvas</code>,
 * it does not make sense to add children to it, or set a layout on it.
 * </p>
 */
//TODO a better implementation is needed; check memory leaks
public class ColorButton extends Canvas {

	/**
	 * RGB color description of the color used to draw a border around the button when mouse is over
	 */
	private static final RGB SELECTED_COLOR = new RGB(0,0,255);

	//colors used by the color button
	private Color backgroundColor = null;
	private Color outerColor = null;
	private Color innerColor = null;
	private Color myWhite = null;
	private Color foregroundUpperColor_myWhite = null;
	private Color foregroundLowerColor_myWhite = null;
	private Color mouseUpWhite = null;
	private Color mouseDownWhite = null;
	private Color selectedColor = null;

	/**
	 * This background image will be shown over the button's background color
	 */
	private Image backgroundImage = null;

	/**
	 * Flag used to indicate if this instance of the color button is selected
	 */
	private boolean isSelected = false;

	/**
	 * Flag to indicate whether the background color should be disposed on widget dispose event
	 */
	private boolean isBackgroundColorDisposable = false;

	/**
	 * Creates a new instance of this class given it's parent composite
	 * and a style value describing its behavior and appearance.
	 * <p>
	 * The style value is either one of the style constants defined in
	 * class <code>SWT</code> which is applicable to instances of this
	 * class, or must be built by <em>bitwise OR</em>'ing together 
	 * (that is, using the <code>int</code> "|" operator) two or more
	 * of those <code>SWT</code> style constants. The class description
	 * lists the style constants that are applicable to the class.
	 * Style bits are also inherited from superclasses.
	 * </p>
	 * 
	 * @param parent the parent for the button
	 * @param style the button's style
	 * 
	 * @see SWT
	 * @see Widget#getStyle
	 */
	public ColorButton(Composite parent, int style) {
		super(parent, style);
		initialize();
	}

	/**
	 * initialize the color button
	 */
	private void initialize() {
		setLayout(new GridLayout());

		backgroundColor = new Color(Display.getCurrent(), 239, 235, 231);

		outerColor = new Color(Display.getCurrent(), 177, 165, 152);
		innerColor = new Color(Display.getCurrent(), 228, 224, 220);

		myWhite = new Color(Display.getCurrent(), 250, 250, 250);
		foregroundUpperColor_myWhite = new Color(Display.getCurrent(), getUpperColor(myWhite));
		foregroundLowerColor_myWhite = new Color(Display.getCurrent(), getLowerColor(myWhite));

		mouseUpWhite = myWhite;
		mouseDownWhite = new Color(Display.getCurrent(), 220, 220, 220);

		selectedColor = new Color(Display.getCurrent(), SELECTED_COLOR);

		//dispose unnecessary resources when widget disposed
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				ColorButton.this.widgetDisposed(e);
			}
		});

		//all the paint for the button get's done here
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				GC gc = e.gc;
				gc.setBackground(backgroundColor);
				int width = getSize().x-1;
				int height = getSize().y-1;

				//draw 2px borders around button
				drawBorders(gc, width, height, outerColor, innerColor);

				gc.getForeground().dispose();
				Color fgColor = new Color(Display.getCurrent(), getUpperColor(backgroundColor));
				gc.setForeground(fgColor);
				gc.fillGradientRectangle(2, 2, width-3, (height-3)/2, true);

				fgColor.dispose();
				fgColor = new Color(Display.getCurrent(), getLowerColor(backgroundColor));
				gc.setForeground(fgColor);
				gc.fillGradientRectangle(2, 2 + ((height-3)/2), width-3, (height-2)/2, true);
				fgColor.dispose();

				if (backgroundImage != null) {
					gc.drawImage(backgroundImage, 0, 0, backgroundImage.getBounds().width, backgroundImage.getBounds().height, 2, 2, width - 3, height - 3);
				}

			}
		});

		this.addMouseTrackListener(new org.eclipse.swt.events.MouseTrackAdapter() {   
			public void mouseExit(org.eclipse.swt.events.MouseEvent e) {
				setBackground(backgroundColor);
				redraw();
			}
			public void mouseEnter(org.eclipse.swt.events.MouseEvent e) {
				GC gc = new GC(getAccessible().getControl());

				int width = getSize().x-1;
				int height = getSize().y-1;
				gc.setBackground(myWhite);

				//draw 2px borders around button
				drawBorders(gc, width, height, outerColor, backgroundColor);

				gc.setForeground(foregroundUpperColor_myWhite);
				gc.fillGradientRectangle(2, 2, width-3, (height-3)/2, true);

				gc.setForeground(foregroundLowerColor_myWhite);
				gc.fillGradientRectangle(2, 2 + ((height-3)/2), width-3, (height-2)/2, true);

				if (backgroundImage != null) {
					gc.drawImage(backgroundImage, 0, 0, backgroundImage.getBounds().width, backgroundImage.getBounds().height, 2, 2, width - 3, height - 3);
				}
				gc.dispose();
			}
		});
		this.addMouseListener(new org.eclipse.swt.events.MouseAdapter() {   
			public void mouseUp(org.eclipse.swt.events.MouseEvent e) {
				//TODO a better way to detect if event was generated artificially
				if (e.button != 0) { //paint only if is a real mouse up event (not a generated one)
					paintWith(mouseUpWhite);
				}
			}
			public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
				isSelected = true;
				paintWith(mouseDownWhite);
			}
		});
	}

	/**
	 * Dispose the used resources on widget disposed event
	 */
	protected void widgetDisposed(DisposeEvent e) {

		if (isBackgroundColorDisposable) {
			backgroundColor.dispose();
		}
		outerColor.dispose();
		innerColor.dispose();
		myWhite.dispose();
		foregroundUpperColor_myWhite.dispose();
		foregroundLowerColor_myWhite.dispose();
		mouseUpWhite.dispose();
		mouseDownWhite.dispose();
		selectedColor.dispose();
	}

	/**
	 * Update the button's borders colors
	 * @param givenGc the graphical context to be used
	 * @param width the width of the borders
	 * @param height the height of the borders
	 * @param outer the border's outer color
	 * @param inner the border's inner color
	 */
	private void drawBorders(GC gc, int width, int height, Color outer, Color inner) {
		if (isSelected) {
			outer = selectedColor;
		}
		gc.setForeground(outer);
		gc.drawRectangle(0, 0, width, height);
		gc.setForeground(inner);
		gc.drawRectangle(1, 1, width-2, height-2);
	}

	@Override
	public void setBackground(Color bkColor) {
		//the first time setBackground is called dispose the Color created in constructor
		if (isBackgroundColorDisposable) {
			backgroundColor.dispose();
		}
		isBackgroundColorDisposable = false;
		backgroundColor = bkColor;
	}

	@Override
	public Color getBackground() {
		return backgroundColor;
	}

	@Override
	public void setBackgroundImage(Image image) {
		backgroundImage = image;
	}

	@Override
	public Image getBackgroundImage() {
		return backgroundImage;
	}

	/**
	 * Given a color return a brighter RGB color description
	 * @param color the color
	 * @return the brighter RGB color description
	 */
	private RGB getUpperColor(Color color) {
		int newRed = color.getRed() + 10 > 255 ? 255 : color.getRed() + 10;
		int newGreen = color.getGreen() + 10 > 255 ? 255 : color.getGreen() + 10;
		int newBlue = color.getBlue() + 10 > 255 ? 255 : color.getBlue() + 10;
		return new RGB(newRed,newGreen,newBlue);
	}

	/**
	 * Given a color return a darker RGB color description
	 * @param color the color
	 * @return the darker RGB color description
	 */
	private RGB getLowerColor(Color color) {
		int newRed = color.getRed() - 10 < 0 ? 0 : color.getRed() - 10;
		int newGreen = color.getGreen() - 10 < 0 ? 0 : color.getGreen() - 10;
		int newBlue = color.getBlue() - 10 < 0 ? 0 : color.getBlue() - 10;
		return new RGB(newRed,newGreen,newBlue);
	}

	/**
	 * Paint the button with the given color
	 * @param color the color to paint the button
	 */
	private void paintWith(Color color) {
		GC gc = new GC(getAccessible().getControl());
		int width = getSize().x-1;
		int height = getSize().y-1;
		gc.setBackground(color);

		//draw 2px borders around button
		drawBorders(gc, width, height, outerColor, backgroundColor);

		gc.getForeground().dispose();
		Color fgColor = new Color(Display.getCurrent(), getUpperColor(color));
		gc.setForeground(fgColor);
		gc.fillGradientRectangle(2, 2, width-3, (height-3)/2, true);
		fgColor.dispose();

		fgColor = new Color(Display.getCurrent(), getLowerColor(color));
		gc.setForeground(fgColor);
		gc.fillGradientRectangle(2, 2 + ((height-3)/2), width-3, (height-2)/2, true);
		fgColor.dispose();

		if (backgroundImage != null) {
			gc.drawImage(backgroundImage, 0, 0, backgroundImage.getBounds().width, backgroundImage.getBounds().height, 2, 2, width - 3, height - 3);
		}
		gc.dispose();
	}

	/**
	 * Change the selection state of this button
	 * @param isSelected the new button state
	 */
	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
		if (!isSelected) {
			redraw();
		}
	}
}