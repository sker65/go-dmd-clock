package com.rinke.solutions.pinball.widget;

import static com.rinke.solutions.pinball.widget.SWTUtil.toModelRGB;
import static com.rinke.solutions.pinball.widget.SWTUtil.toSwtRGB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.widget.color.ColorPicker;
import com.rinke.solutions.pinball.widget.color.ColorPicker.ColorModifiedEvent;
import com.rinke.solutions.pinball.widget.color.ColorPicker.ColorModifiedListener;

public class PaletteTool implements ColorModifiedListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(PaletteTool.class);

	final ToolItem colBtn[] = new ToolItem[16];
	Palette palette;
	private Display display;

	ResourceManager resManager;
	private int selectedColor;
	byte[] visible = { 1, 1, 0, 0, 0, 0, 0, 1, 
			           0, 0, 0, 0, 0, 0, 0, 1 };
	private ToolBar paletteBar;

	/** used to reused images in col buttons. */
	Map<RGB, Image> colImageCache = new HashMap<>();
	List<ColorChangedListerner> listeners = new ArrayList<>();
	ColorPicker colorPicker = new ColorPicker();

	@FunctionalInterface
	public static interface ColorChangedListerner {
		public void paletteChanged(Palette palette );
	}

	public void addListener(ColorChangedListerner listener) {
		listeners.add(listener);
	}

	public void redraw() {
		paletteBar.redraw();
	}

	public PaletteTool(Shell shell, Composite parent, int flags, Palette palette) {
		this.palette = palette;
		resManager = new LocalResourceManager(JFaceResources.getResources(),
				parent);
		paletteBar = new ToolBar(parent, flags);
		GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
		gd.widthHint = 310;
		paletteBar.setLayoutData(gd);
		createColorButtons(paletteBar, 20, 10, palette);
		colorPicker.addListener(this);
	}

	public void setNumberOfPlanes(int planes) {
		switch (planes) {
		case 1:
			for (int i = 0; i < colBtn.length; i++)
				colBtn[i].setEnabled(i<2);
			break;
		case 2: // 2 planes -> 4 colors
			for (int i = 0; i < colBtn.length; i++)
				colBtn[i].setEnabled(visible[i] == 1);
			break;
		case 4: // 4 planes -> 16 colors
			for (int i = 0; i < colBtn.length; i++)
				colBtn[i].setEnabled(true);
			break;
		}
	}

	Image getSquareImage(Display display, RGB rgb) {
		Image image = colImageCache.get(rgb);
		if (image == null) {
			image = resManager.createImage(ImageDescriptor
					.createFromImage(new Image(display, 12, 12)));
			GC gc = new GC(image);
			Color col = new Color(display, rgb);
			gc.setBackground(col);
			gc.fillRectangle(0, 0, 11, 11);
			Color fg = new Color(display, 0, 0, 0);
			gc.setForeground(fg);
			gc.drawRectangle(0, 0, 11, 11);
			// gc.setBackground(col);
			fg.dispose();
			gc.dispose();
			col.dispose();
			colImageCache.put(rgb, image);
		}
		return image;
	}

	private void createColorButtons(ToolBar toolBar, int x, int y, Palette pal) {
		for (int i = 0; i < colBtn.length; i++) {
			colBtn[i] = new ToolItem(toolBar, SWT.RADIO);
			colBtn[i].setData(Integer.valueOf(i));
			colBtn[i].setImage(getSquareImage(display, toSwtRGB(pal.colors[i])));
			colBtn[i].addListener(SWT.Selection, e -> {
				int col = (Integer) e.widget.getData();
				selectedColor = col;
				tmpRgb = getSelectedRGB();
				boolean sel = ((ToolItem)e.widget).getSelection();
				//listeners.forEach(l -> l.setActualColorIndex(selectedColor));
				if( sel && ( (e.stateMask & SWT.CTRL) != 0 || (e.stateMask & 4194304) != 0 )) {
					changeColor();
				}
			});

		}
	}

	public int getSelectedColor() {
		return selectedColor;
	}

	public RGB getSelectedRGB() {
		return toSwtRGB(palette.colors[selectedColor]);
	}

	public void setPalette(Palette palette) {
		this.palette = palette;
		for (int i = 0; i < colBtn.length; i++) {
			colBtn[i].setImage(getSquareImage(display, toSwtRGB(palette.colors[i])));
		}
	}
	
	RGB tmpRgb = new RGB(0,0,0);
	
	public void changeColor() {
		tmpRgb = getSelectedRGB();
		LOG.info("changing color, old color was. {}", tmpRgb);
		RGB rgb = colorPicker.open();
		if( rgb != null) {
			LOG.info("updating to new color: {}", rgb);
			updateSelectedColor(rgb);
		} else {
			LOG.info("restoring old color.");
			updateSelectedColor(tmpRgb);
		}
	}

	private void updateSelectedColor(RGB rgb) {
		palette.colors[selectedColor] = toModelRGB(rgb);
		colBtn[selectedColor].setImage(getSquareImage(display, rgb));
		listeners.forEach(l -> l.paletteChanged(palette));
	}

	@Override
	public void colorModified(ColorModifiedEvent evt) {
		switch(evt.eventType) {
		case Choosing:
			updateSelectedColor(evt.rgb);
			break;
		case Selected:
			updateSelectedColor(evt.rgb);
			break;
		}
	}

}
