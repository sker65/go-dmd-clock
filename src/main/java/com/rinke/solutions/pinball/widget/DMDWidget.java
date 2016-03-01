package com.rinke.solutions.pinball.widget;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.widget.PaletteTool.ColorChangedListerner;

import static com.rinke.solutions.pinball.widget.SWTUtil.toSwtRGB;

public class DMDWidget extends ResourceManagedCanvas implements ColorChangedListerner {
	
	private Palette palette;	// color palette
	private DMD dmd; 			// the model holding buffers etc.
	private DMD mask;			// if set draw on mask and paint a mask overlay
	private int resolutionX;
	private int resolutionY;
	private int bytesPerRow;
	int margin = 20;
	int pitch = 7;
	int pressedButton = 0;
	private DrawTool drawTool = null;//new RectTool();//new SetPixelTool();
	private boolean drawingEnabled;

	public DMDWidget(Composite parent, int style, DMD dmd) {
		super(parent, style);
		//palette = Palette.getDefaultPalette();
		resolutionX = dmd.getWidth();
		resolutionY = dmd.getHeight();
		bytesPerRow = dmd.getBytesPerRow();
		this.dmd = dmd;
		this.addListener( SWT.MouseDown, e -> handleMouse(e));
		this.addListener( SWT.MouseUp, e -> handleMouse(e));
		this.addListener( SWT.MouseMove, e -> handleMouse(e));
		if( drawTool != null ) drawTool.setDMD(dmd);
	}
	
	private void handleMouse(Event e) {
		if( drawTool != null && drawingEnabled ) {
			int x = (e.x-margin)/pitch;
			int y = (e.y-margin)/pitch;
			//System.out.println(x+ ":"+y);
			if( x >= 0 && x < dmd.getWidth() && y>=0 && y < dmd.getHeight() ) {
				if( drawTool.handleMouse(e, x, y)) {
					redraw();
				}
			}
		}
	}


	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		int minwh = width<height?width:height;
		margin = minwh/25;
		int pitchx = (width -2*margin) / resolutionX;
		int pitchy = (height -2*margin) / resolutionY;
		pitch = pitchx<pitchy?pitchx:pitchy;
		if( pitch <= 0) pitch = 1;
	}

	@Override
	protected void paintWidget(PaintEvent ev) {
		Image image = drawImage(ev.display, ev.width, ev.height);
        ev.gc.drawImage(image, 0, 0);
        image.dispose();
	}
	
	public RGB transformColor( com.rinke.solutions.pinball.model.RGB rgb, boolean dimColors ) {
		if( dimColors ) {
			RGB res = new RGB(rgb.red, rgb.green, rgb.blue);
			float[] hsb = res.getHSB();
			hsb[2] /= 1.6;
			return new RGB(hsb[0],hsb[1],hsb[2]);
		} else {
			return toSwtRGB(rgb);
		}
	}
	
	public Image drawImage(Display display,int w, int h) {
        // int colIdx[] = {0,1,4,15};
    	int numberOfSubframes = dmd.getNumberOfSubframes();
    	boolean useColorIndex = numberOfSubframes < 8;
        Color cols[] = {};
        if( useColorIndex ) {
            cols = new Color[1<<numberOfSubframes];
            if( numberOfSubframes == 2) {
				cols[0] = resourceManager.createColor(toSwtRGB(palette.colors[0]));
                cols[1] = resourceManager.createColor(toSwtRGB(palette.colors[1]));
                cols[2] = resourceManager.createColor(toSwtRGB(palette.colors[4]));
                cols[3] = resourceManager.createColor(toSwtRGB(palette.colors[15]));
            } else {
                for(int i = 0; i < (1 << numberOfSubframes);i++) {
                    cols[i] = resourceManager.createColor(toSwtRGB(palette.colors[i]));
                }
            }
        }

        Color bg = resourceManager.createColor(new RGB(10, 10, 10));

		Image image =  new Image(display, w, h);		
        GC gcImage = new GC(image);
        gcImage.setBackground(bg);
        gcImage.fillRectangle(0, 0, w, h);

        drawDMD(gcImage, dmd.getFrame(), numberOfSubframes, useColorIndex, cols);
        if( mask != null ) {
            ImageData imageData = image.getImageData();
    		imageData.alpha = 96;
    		Image maskImage =  new Image(display, imageData);
            GC gcMask = new GC(maskImage);
			cols[0] = resourceManager.createColor(new RGB(0, 0, 0));
            cols[1] = resourceManager.createColor(new RGB(255, 0, 0));
            drawDMD(gcMask, mask.getFrame(), 1, true, cols);
            gcImage.drawImage(maskImage, 0, 0);
            gcMask.dispose();
        }
		
        gcImage.dispose();
        return image;
	}

	private void drawDMD(GC gcImage, Frame frame, int numberOfSubframes,
			boolean useColorIndex, Color[] cols) {
		for (int row = 0; row < resolutionY; row++) {
            for (int col = 0; col < resolutionX; col++) {
                // lsb first
                // byte mask = (byte) (1 << (col % 8));
                // hsb first
                byte mask = (byte) (128 >> (col % 8));
                int v = 0;
                for(int i = 0; i < numberOfSubframes;i++) {
                    v += (frame.getPlaneBytes(i)[col / 8 + row * bytesPerRow] & mask) != 0 ? (1<<i) : 0;
                }

                if( useColorIndex ) {
                	gcImage.setBackground(cols[v]);
                } else {
                	// v is rgb directly
                	int r = (v >> 8) << 4;
                    int g = ( ( v >> 4 ) & 0xF ) << 4;
                    int b = ( v & 0x0F ) << 4;
                	Color c = resourceManager.createColor(new RGB(r,g,b));
                	gcImage.setBackground(c);
                }
                gcImage.fillOval(margin + col * pitch, margin + row * pitch, pitch, pitch);
            }
        }
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		return new Point(wHint,hHint);
	}

	public Palette getPalette() {
		return palette;
	}

	public void setPalette(Palette palette) {
		this.palette = palette;
		redraw();
	}

	public DrawTool getDrawTool() {
		return drawTool;
	}

	public void setDrawTool(DrawTool drawTool) {
		this.drawTool = drawTool;
		if(drawTool!= null) {
			if( mask == null ) {
				this.drawTool.setDMD(dmd);
			} else {
				this.drawTool.setDMD(mask);
			}
		}
	}

	public boolean isDrawingEnabled() {
		return drawingEnabled;
	}

	public void setDrawingEnabled(boolean drawingEnabled) {
		this.drawingEnabled = drawingEnabled;
	}

	@Override
	public void setActualColorIndex(int actualColorIndex) {
		
	}

	@Override
	public void paletteChanged(Palette pal) {
		setPalette(pal);
	}

	public DMD getMask() {
		return mask;
	}

	public void setMask(DMD mask) {
		this.mask = mask;
		redraw();
	}


}
