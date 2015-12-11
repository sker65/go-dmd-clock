package com.rinke.solutions.pinball.widget;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.rinke.solutions.pinball.model.Palette;

public class PaletteTool {

	final ToolItem colBtn[] = new ToolItem[16];
	RGB[] rgb;
	private Display display;
	
	ResourceManager resManager;
	private int selectedColor;
	
    /** used to reused images in col buttons. */
    Map<RGB,Image> colImageCache = new HashMap<>();
    List<ColorIndexChangedListerner> listeners = new ArrayList<>();
    
    public interface ColorIndexChangedListerner {
    	public void setActualColor(int actualColor);
    }
    
    public void addListener( ColorIndexChangedListerner listener) {
    	listeners.add(listener);
    }

	public PaletteTool(Composite parent, int flags, RGB[] rgb1) {
		this.rgb = Arrays.copyOf(rgb1, rgb1.length);
		resManager = new LocalResourceManager(JFaceResources.getResources(),parent);
        ToolBar paletteBar = new ToolBar(parent, flags);
        GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
        gd.widthHint = 420;
        paletteBar.setLayoutData(gd);
        createColorButtons(paletteBar,20,10, rgb);
	}

	public void setColor( RGB in) {
		rgb[selectedColor] = new RGB(in.red, in.green, in.blue);
		colBtn[selectedColor].setImage(getSquareImage(display, in));
	}
	
    byte[] visible = { 1,1,0,0, 1,0,0,0, 0,0,0,0, 0,0,0,1 };
    
    public void  planesChanged(int planes) {
        switch(planes) {
        case 0: // 2 planes -> 4 colors
            for(int i = 0; i < colBtn.length; i++) colBtn[i].setEnabled(visible[i]==1); 
            break;
        case 1: // 4 planes -> 16 colors
            for(int i = 0; i < colBtn.length; i++) colBtn[i].setEnabled(true);
            break;  
        }
    }
	
	Image getSquareImage(Display display, RGB rgb) {
	    Image image = colImageCache.get(rgb);
	    if( image == null ) {
	        image = resManager.createImage(ImageDescriptor.createFromImage(new Image(display, 12, 12)));
	        GC gc = new GC(image);
	        Color col = new Color(display, rgb);
	        gc.setBackground(col);
	        gc.fillRectangle(0, 0, 11, 11);
	        Color fg = new Color(display,0,0,0);
	        gc.setForeground(fg);
	        gc.drawRectangle(0, 0, 11, 11);
	        //gc.setBackground(col);
	        fg.dispose();
	        gc.dispose();
	        col.dispose();
	        colImageCache.put(rgb, image);
	    }
        return image;
    }
	 
    private void createColorButtons(ToolBar toolBar, int x, int y, RGB[] rgb) {
        for(int i = 0; i < colBtn.length; i++) {
            colBtn[i] = new ToolItem(toolBar, SWT.RADIO);
            colBtn[i].setData(Integer.valueOf(i));
            colBtn[i].setImage(getSquareImage(display, rgb[i]));
            colBtn[i].addListener(SWT.Selection, e -> {
                selectedColor = (Integer) e.widget.getData();
                listeners.forEach(l->l.setActualColor(selectedColor));
            });
        }
    }

	public int getSelectedColor() {
		return selectedColor;
	}

	public RGB getSelectedRGB() {
		return rgb[selectedColor];
	}

	public void setPalette(Palette palette) {
        for (int i = 0; i < colBtn.length; i++) {
            colBtn[i].setImage(getSquareImage(display, rgb[i]));
        }
	}
    
}
