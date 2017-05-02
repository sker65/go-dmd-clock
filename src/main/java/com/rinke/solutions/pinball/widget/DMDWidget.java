package com.rinke.solutions.pinball.widget;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.widget.PaletteTool.ColorChangedListerner;

import static com.rinke.solutions.pinball.widget.SWTUtil.toSwtRGB;

public class DMDWidget extends ResourceManagedCanvas implements ColorChangedListerner {
	
	private Palette palette;	// color palette
	private DMD dmd; 			// the model holding buffers etc.
	private boolean showMask;			// if set draw on mask and paint a mask overlay
	private int resolutionX;
	private int resolutionY;
	private int bytesPerRow;
	int margin = 20;
	int pitch = 7;
	int pressedButton = 0;
	private DrawTool drawTool = null;//new RectTool();//new SetPixelTool();
	private DrawTool previousDrawTool;
	private boolean drawingEnabled;
	private ScrollBar hBar;
	private ScrollBar vBar;
	int vScroll;
	int hScroll;
	private int width;
	private int height;
	private boolean scrollable = false;
	private List<FrameChangedListerner> frameChangedListeners = new ArrayList<>();
	private boolean maskLocked;
	private boolean maskOut = false;
	// define the rubberband area for copy
	private RGB areaColorNormal = new RGB(255,255,0);
	private RGB areaColorHighlighted = new RGB(255,255,255);
	private RGB areaColor = areaColorNormal;
	boolean mouseOnAreaMark = false;
	private Rect selection;
	private Cursor cursor;
	private boolean inSelection;
	
	@FunctionalInterface
	public static interface FrameChangedListerner {
		public void frameChanged(Frame frame);
	}
	
	public DMDWidget(Composite parent, int style, DMD dmd, boolean scrollable) {
		super(parent, style + ( scrollable ? SWT.V_SCROLL + SWT.H_SCROLL : 0));
		this.scrollable = scrollable;
		setResolution(dmd);
		//palette = Palette.getDefaultPalette();
		this.addListener( SWT.MouseDown, e -> handleMouse(e));
		this.addListener( SWT.MouseUp, e -> handleMouse(e));
		this.addListener( SWT.MouseMove, e -> handleMouse(e));
		if( scrollable ) {
			hBar = this.getHorizontalBar();
			vBar = this.getVerticalBar();
			hBar.addListener(SWT.Selection, e->resized(e));
			vBar.addListener(SWT.Selection, e->resized(e));
		}
		this.addDisposeListener(e->{if(this.cursor!=null) this.cursor.dispose();});
		
		//areaX = 10; areaY = 2; areaW = 30; areaH = 10;
		
		if( drawTool != null ) drawTool.setDMD(dmd);
	}
	
	public void setSelection( int x, int y, int w, int h) {
		if( w==0 && h==0)
			this.selection = null;
		else
			this.selection = new Rect( x,y,x+w,y+h);
		redraw();
	}
	
	public void addListeners( FrameChangedListerner l) {
		frameChangedListeners.add(l);
	}
	
	public void setResolution(DMD dmd) {
		resolutionX = dmd.getWidth();
		resolutionY = dmd.getHeight();
		bytesPerRow = dmd.getBytesPerRow();
		this.dmd = dmd;
	}

	private void resized(Event e) {
		hScroll = hBar.getSelection();
		vScroll = vBar.getSelection();
		//System.out.println(hScroll);
		redraw();
	}
	
	private void handleMouse(Event e) {
		// calc dmd coords
		int x = (e.x-margin)/pitch + hScroll;
		int y = (e.y-margin)/pitch + vScroll;
		if( isSelectionSet() ) {
			if( selection.isOnSelectionMark(x, y) ) {
				if( !mouseOnAreaMark ) {
					areaColor = areaColorNormal;
					redraw();
					mouseOnAreaMark = true;
				} 
			} else {
				if( mouseOnAreaMark ) {
					areaColor = areaColorHighlighted;
					redraw();
					mouseOnAreaMark = false;
				}
				Shell shell = Display.getCurrent().getActiveShell();
				if( selection.inSelection(x, y) && !inSelection ) {
					if( this.cursor != null ) cursor.dispose();
					shell.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_SIZEALL));
					inSelection = true;
				} 
				if( !selection.inSelection(x, y) && inSelection ) {
					if( this.cursor != null ) cursor.dispose();
					shell.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW));
					inSelection = false;
				}
			}
		}
		if( drawTool != null && drawingEnabled ) {
			//if( x >= 0 && x < dmd.getWidth() && y>=0 && y < dmd.getHeight() ) {
				if( drawTool.handleMouse(e, x, y)) {
					redraw();
					frameChangedListeners.forEach(l->l.frameChanged(dmd.getFrame()));
				}
			//}
		}
	}
	
	public void resetSelection() {
		setSelection(0, 0, 0, 0);
		restorePreviousDrawTool();
	}
	
	public boolean isSelectionSet() {
		return selection!=null;
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		this.width = width;
		this.height = height;
		int minwh = width<height?width:height;
		margin = minwh/25;
		int pitchx = (width -2*margin) / resolutionX;
		int pitchy = (height -2*margin) / resolutionY;
		pitch = pitchx<pitchy?pitchx:pitchy;
		if( pitch <= 0) pitch = 1;
		setBars();
	}

	private void setBars() {
		if( !scrollable  ) return;
		int xoff = resolutionX*pitch + 2*margin - width;
		int yoff = resolutionY*pitch + 2*margin - height;
		hBar.setMaximum(xoff<=0?1:xoff / pitch + 2);
		vBar.setMaximum(yoff<=0?1:yoff/pitch+2);
	}
	
	public void incPitch() {
		pitch++;
		setBars();
		redraw();
	}

	public void decPitch() {
		if (pitch > 1) {
			pitch--;
			setBars();
		}
		redraw();
	}

	@Override
	protected void paintWidget(PaintEvent ev) {
		Image image = drawImage(ev.display, margin*2+resolutionX*pitch, margin*2+resolutionY*pitch);
        ev.gc.drawImage(image, -hScroll*pitch, -vScroll*pitch);
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
    	int numberOfSubframes = dmd.getNumberOfPlanes();
    	boolean useColorIndex = numberOfSubframes < 8;
        Color cols[] = {};
        if( showMask ) cols = new Color[1<<numberOfSubframes];
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
        if( showMask && dmd.getFrame().mask != null) {
            drawMask(display, cols, image, gcImage);
        }
        if( isSelectionSet() && !isShowMask() ) {
            drawSelection(display, image, gcImage);
        }
		
        gcImage.dispose();
        return image;
	}

	private void drawMask(Display display, Color[] cols, Image image, GC gcImage) {
		ImageData imageData = image.getImageData();
		imageData.alpha = 96;
		Image maskImage =  new Image(display, imageData);
		GC gcMask = new GC(maskImage);
		cols[0] = resourceManager.createColor(new RGB(0, 0, 0));
		cols[1] = resourceManager.createColor(maskLocked ? new RGB(255, 0, 0) : new RGB(0, 0, 255));
		// create a fake mask frame
		Frame maskFrame = new Frame(dmd.getFrame().mask.data, dmd.getFrame().mask.data);
		drawDMD(gcMask, maskFrame, 1, true, cols);
		gcImage.drawImage(maskImage, 0, 0);
		gcMask.dispose();
	}

	private void drawSelection(Display display, Image image, GC gcImage) {
		ImageData imageData = image.getImageData();
		imageData.alpha = 128;
		Image rubberImage =  new Image(display, imageData);
		GC gcRubber = new GC(rubberImage);  
		Color color = resourceManager.createColor(areaColor);
		int lineWidth = 3;
		gcRubber.setLineWidth(lineWidth);
		gcRubber.setLineStyle(SWT.LINE_DOT);
		gcRubber.setForeground(color);
		gcRubber.drawRectangle(selection.x1*pitch+margin, selection.y1*pitch+margin, (selection.x2-selection.x1)*pitch-lineWidth, (selection.y2-selection.y1)*pitch-lineWidth);
		if( isMouseOnAreaMark() ) {
			gcRubber.setLineStyle(SWT.LINE_SOLID);
			int handleSize = 8;
			gcRubber.drawRectangle((selection.x1 + ( selection.x2-selection.x1) / 2)*pitch+margin-(handleSize/2),
					selection.y1*pitch+margin-(handleSize/2), handleSize, handleSize);

			gcRubber.drawRectangle((selection.x1 + ( selection.x2-selection.x1) / 2)*pitch+margin-(handleSize/2),
					selection.y2*pitch+margin-handleSize, handleSize, handleSize);
			
			gcRubber.drawRectangle(selection.x1*pitch+margin-(handleSize/2),
					(selection.y1+(selection.y2-selection.y1)/2)*pitch+margin-(handleSize/2), handleSize, handleSize);
			
			gcRubber.drawRectangle(selection.x2 *pitch+margin-(handleSize/1),
					(selection.y1+(selection.y2-selection.y1)/2)*pitch+margin-(handleSize/2), handleSize, handleSize);
		}
		gcImage.drawImage(rubberImage, 0, 0);
		gcRubber.dispose();
	}

	private void drawDMD(GC gcImage, Frame frame, int numberOfSubframes, boolean useColorIndex, Color[] cols) {
		boolean checkMask = maskOut && frame.hasMask();
		byte[] maskData = checkMask?frame.mask.data:null;
		for (int row = 0; row < resolutionY; row++) {
            for (int col = 0; col < resolutionX; col++) {
                // lsb first
                // byte mask = (byte) (1 << (col % 8));
                // hsb first
                byte mask = (byte) (0b10000000 >> (col % 8));
                int v = 0;
                for(int i = 0; i < numberOfSubframes;i++) {
                	if( col / 8 + row * bytesPerRow < frame.getPlane(i).length) {
                		if( checkMask ) {
                			if( (maskData[col / 8 + row * bytesPerRow] & mask) != 0) 
                				v += (frame.getPlane(i)[col / 8 + row * bytesPerRow] & mask) != 0 ? (1<<i) : 0;
                		} else {
                			v += (frame.getPlane(i)[col / 8 + row * bytesPerRow] & mask) != 0 ? (1<<i) : 0;
                		}
                	}
                }

                if( useColorIndex ) {
                	gcImage.setBackground(cols[v]);
                } else {
                	// v is rgb directly
                	int r = (v >> 10) << 3;
                    int g = ( ( v >> 5 ) & 0x1F ) << 3;
                    int b = ( v & 0x1F ) << 3;
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
	
	private void restorePreviousDrawTool() {
		this.drawTool = previousDrawTool;
		if(drawTool!= null) {
			this.drawTool.setDMD(dmd);
		}
	}

	public void setDrawTool(DrawTool drawTool) {
		previousDrawTool = this.drawTool;
		this.drawTool = drawTool;
		if(drawTool!= null) {
			previousDrawTool = this.drawTool;
			this.drawTool.setDMD(dmd);
			if( drawTool instanceof SelectTool ) {
				SelectTool sel = (SelectTool) drawTool;
				setSelection(sel.getSelection());
			} else {
				resetSelection();
			}
		}
	}

	public void setSelection(Rect s) {
		setSelection(s.x1, s.y1, s.x2-s.x1, s.y2-s.y1);
	}

	public boolean isDrawingEnabled() {
		return drawingEnabled;
	}

	public void setDrawingEnabled(boolean drawingEnabled) {
		this.drawingEnabled = drawingEnabled;
	}

	@Override
	public void paletteChanged(Palette pal) {
		setPalette(pal);
	}

	public void setMaskLocked(boolean maskLocked) {
		this.maskLocked = maskLocked;
		if( drawTool != null ) setDrawTool(drawTool);
		redraw();
	}

	public void setShowMask(boolean showMask) {
		boolean old = this.showMask;
		this.showMask = showMask;
		if( old != showMask ) {
			redraw();
		}
	}

	public boolean isShowMask() {
		return showMask;
	}

	public void setMask(Mask mask) {
		dmd.setMask(mask.data);
		this.setShowMask(true);
		this.setMaskLocked(mask.locked);
	}

	public void setMaskOut(boolean maskOut) {
		 this.maskOut = maskOut;
	}

	public boolean isMouseOnAreaMark() {
		return mouseOnAreaMark;
	}
	
	public static class Rect {
		public final int x1,y1,x2,y2;
		public Rect(int x1, int y1, int x2, int y2) {
			super();
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
		
		public boolean inSelection(int x, int y) {
			return x>=x1 && x< x2 && y>=y1 && y<y2;
		}
		
		public boolean isOnSelectionMark(int x, int y) {
			return x == this.x1 || y == this.y1 || x == this.x2-1 || y == this.y2-1;
		}
		
		public static boolean selected( Rect sel, int x, int y) {
			return sel==null||sel.inSelection(x, y);
		}
		@Override
		public String toString() {
			return String.format("Rect [x1=%s, y1=%s, x2=%s, y2=%s]", x1, y1, x2, y2);
		}
	}

	public Rect getSelection() {
		return this.selection;
	}
}
