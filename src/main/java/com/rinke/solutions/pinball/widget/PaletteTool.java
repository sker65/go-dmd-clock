package com.rinke.solutions.pinball.widget;

import static com.rinke.solutions.pinball.widget.SWTUtil.toModelRGB;
import static com.rinke.solutions.pinball.widget.SWTUtil.toSwtRGB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.pinball.Constants;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.CmdDispatcher.Command;
import com.rinke.solutions.pinball.view.model.AbstractModel;
import com.rinke.solutions.pinball.widget.color.ColorPicker;
import com.rinke.solutions.pinball.widget.color.ColorPicker.ColorModifiedEvent;
import com.rinke.solutions.pinball.widget.color.ColorPicker.ColorModifiedListener;

@Slf4j
public class PaletteTool extends AbstractModel implements ColorModifiedListener {
	
	final ToolItem colBtn[] = new ToolItem[64];
	Palette palette;
	private Display display;

	@Autowired CmdDispatcher dispatcher;

	ResourceManager resManager;
	private int selectedColor;
	byte[] visible = { 1, 1, 0, 0, 1, 0, 0, 0, 
			           0, 0, 0, 0, 0, 0, 0, 1 };
    private ToolBar paletteBar16;
    private ToolBar paletteBar32;
    private ToolBar paletteBar48;
    private ToolBar paletteBar64;
    
	/** used to reused images in col buttons. */
	Map<RGB, Image> colImageCache = new HashMap<>();
	List<ColorChangedListener> colorChangedListeners = new ArrayList<>();
	List<ColorIndexChangedListener> indexChangedListeners = new ArrayList<>();
	ColorPicker colorPicker = new ColorPicker(Display.getDefault(), null);
	// just for the sake of POJO spec
	@Getter private int numberOfPlanes;

	@FunctionalInterface
	public static interface ColorChangedListener {
		public void paletteChanged(Palette palette );
	}
	
	@FunctionalInterface
	public static interface ColorIndexChangedListener {
		public void indexChanged(int index);
	}

	public void addListener(ColorChangedListener listener) {
		if( listener != null ) colorChangedListeners.add(listener);
	}
	
	// draw tools bind to this to get actual color index
	public void addIndexListener(ColorIndexChangedListener listener) {
		indexChangedListeners.add(listener);
	}

	public void redraw() {
        paletteBar16.redraw();
        paletteBar32.redraw();
        paletteBar48.redraw();
        paletteBar64.redraw();
	}

    public PaletteTool(Shell shell, Composite parent16, Composite parent32, Composite parent48,Composite parent64, int flags, Palette palette) {
 		this.palette = palette;
		resManager = new LocalResourceManager(JFaceResources.getResources(),
                parent16);
        paletteBar16 = new ToolBar(parent16, flags);
        paletteBar32 = new ToolBar(parent32, flags);
        paletteBar48 = new ToolBar(parent48, flags);
        paletteBar64 = new ToolBar(parent64, flags);
        createColorButtons(paletteBar16, paletteBar32, paletteBar48, paletteBar64, palette);
        colorPicker.addListener(this);
	}

	public void setNumberOfPlanes(int planes) {
		this.numberOfPlanes = planes;
		log.info("setting number of planes: {}",planes);
		for (int i = 0; i < colBtn.length; i++)
			colBtn[i].setEnabled(false);
		switch (planes) {
		case 1:
			for (int i = 0; i < palette.numberOfColors; i++)
				colBtn[i].setEnabled(i<2);
			break;
		case 2: // 2 planes -> 4 colors
			for (int i = 0; i < palette.numberOfColors; i++)
				colBtn[i].setEnabled(visible[i] == 1);
			break;
		case 4: // 4 planes -> 16 colors
			for (int i = 0; i < 16; i++)
				colBtn[i].setEnabled(true);
			break;
		case 6: // 6 planes -> 64 colors
		case Constants.MAX_BIT_PER_COLOR_CHANNEL*3: // 15 planes -> rgb mode
		case Constants.TRUE_COLOR_BIT_PER_CHANNEL*3: // 24 planes
			for (int i = 0; i < palette.numberOfColors; i++)
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

    private void createColorButtons(ToolBar toolBar16,ToolBar toolBar32,ToolBar toolBar48,ToolBar toolBar64, Palette pal) {
        for (int i = 0; i < colBtn.length; i++) {
            if (i < 16)
                colBtn[i] = new ToolItem(toolBar16, SWT.RADIO);
            else if (i < 32)
                colBtn[i] = new ToolItem(toolBar32, SWT.RADIO);
            else if (i < 48)
                colBtn[i] = new ToolItem(toolBar48, SWT.RADIO);
            else if (i < 64)
                colBtn[i] = new ToolItem(toolBar64, SWT.RADIO);

            colBtn[i].setSelection(i==0);
			
			colBtn[i].setData(Integer.valueOf(i));
			if (i < pal.colors.length) {
				colBtn[i].setImage(getSquareImage(display, toSwtRGB(pal.colors[i])));
				colBtn[i].setToolTipText("R: "+ pal.colors[i].red + " G: "+ pal.colors[i].green + " B: "+ pal.colors[i].blue + "\nCtrl-Click to edit\nShift-Click to swap");
			}
			else {
				colBtn[i].setImage(getSquareImage(display, new RGB(255,255,255)));
			}
			
			colBtn[i].addListener(SWT.Selection, e -> {
				int col = (Integer) e.widget.getData();
				int oldCol = selectedColor;
				setSelectedColor(col);
				tmpRgb = getSelectedRGB();
				boolean sel = ((ToolItem)e.widget).getSelection();
				updateColorIndex(selectedColor, tmpRgb);
				if( sel && ( (e.stateMask & SWT.CTRL) != 0 || (e.stateMask & 4194304) != 0 )) {
					changeColor();
				}
				if( sel && ( e.stateMask & SWT.SHIFT) != 0 ) {
					// swap
					com.rinke.solutions.pinball.model.RGB tmp = palette.colors[oldCol];
					palette.colors[oldCol] = palette.colors[selectedColor];
					palette.colors[selectedColor] = tmp;
					setPalette(this.palette);
					swapColor(oldCol,selectedColor);
				}
			});
            if( i % 4 == 3 && i < colBtn.length-1) {
                if (i<16) 
                    new ToolItem(toolBar16, SWT.SEPARATOR);
                else if (i<32) 
                    new ToolItem(toolBar32, SWT.SEPARATOR);
                else if (i<48) 
                    new ToolItem(toolBar48, SWT.SEPARATOR);
                else if (i<64) 
                    new ToolItem(toolBar64, SWT.SEPARATOR);
            }
		}
	}
	
	private int rgbAsInt(RGB rgb, int bitPerChannel) {
		return ((tmpRgb.red>>(8-bitPerChannel))<<(bitPerChannel*2)) | ((tmpRgb.green>>(8-bitPerChannel))<<bitPerChannel) | (tmpRgb.blue>>(8-bitPerChannel));
	}

	void updateColorIndex(int idx, RGB tmpRgb ) {
		int v = this.numberOfPlanes < Constants.MAX_BIT_PER_COLOR_CHANNEL*3 ? selectedColor : rgbAsInt(tmpRgb, Constants.MAX_BIT_PER_COLOR_CHANNEL);
		indexChangedListeners.forEach(l -> l.indexChanged(v));
	}

	private void swapColor(int oldCol, int newCol) {
		dispatcher.dispatch(new Command<Object[]>(new Object[]{oldCol,newCol}, "swapColors"));
	}

	public int getSelectedColor() {
		return selectedColor;
	}

	public RGB getSelectedRGB() {
		return toSwtRGB(palette.colors[selectedColor]);
	}

	public void setPalette(Palette palette) {
		this.palette = palette;
		for (int i = 0; i < palette.colors.length; i++) {
			colBtn[i].setToolTipText("R: "+ palette.colors[i].red + " G: "+ palette.colors[i].green + " B: "+ palette.colors[i].blue + "\nCtrl-Click to edit\nShift-Click to swap");
			colBtn[i].setImage(getSquareImage(display, toSwtRGB(palette.colors[i])));
		}
	}
	
	RGB tmpRgb = new RGB(0,0,0);
	
	public void changeColor() {
		tmpRgb = getSelectedRGB();
		log.info("changing color, old color was. {}", tmpRgb);
		RGB rgb = colorPicker.open(tmpRgb);
		if( rgb != null) {
			log.info("updating to new color: {}", rgb);
			updateSelectedColor(rgb);
			updateColorIndex(selectedColor, rgb);
		} else {
			log.info("restoring old color.");
			updateSelectedColor(tmpRgb);
		}
	}

	private void updateSelectedColor(RGB rgb) {
		palette.colors[selectedColor] = toModelRGB(rgb);
		colBtn[selectedColor].setToolTipText("R: "+ palette.colors[selectedColor].red + " G: "+ palette.colors[selectedColor].green + " B: "+ palette.colors[selectedColor].blue + "\nCtrl-Click to edit\nShift-Click to swap");
		colBtn[selectedColor].setImage(getSquareImage(display, rgb));
		colorChangedListeners.forEach(l -> l.paletteChanged(palette));
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
	public Palette getPalette() {
		return palette;
	}

	public void setSelectedColor(int selectedColor) {
		//avoid multiple selections in palette
		for (int i = 0; i < colBtn.length; i++) {
			colBtn[i].setSelection(false);
		}
		colBtn[selectedColor].setSelection(true);
		firePropertyChange("selectedColor", this.selectedColor, this.selectedColor = selectedColor);
	}

}
