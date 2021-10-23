package com.rinke.solutions.pinball;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.rinke.solutions.pinball.Constants;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.renderer.ImageUtil;
import com.rinke.solutions.pinball.swt.SWTClipboard;
import com.rinke.solutions.pinball.widget.DMDWidget;
import com.rinke.solutions.pinball.widget.DMDWidget.Rect;
import com.rinke.solutions.pinball.widget.DrawTool;
import com.rinke.solutions.pinball.widget.PasteTool;

/**
 * handler class for clipboard actions in the editor (the dmd preview widget). handles copy & paste actions
 * @author Stefan Rinke
 */
@Slf4j
public class ClipboardHandler {
	
	// introduce an interface that eliminates the direct dependencies from SWT clipboard
	ClipboardFacade clipboard; 
	DMDWidget dmdWidget;

	DMD dmd;
	int width;
	int height;
	Palette palette;
	int dx,dy,dx2,dy2;
	
	/**
	 * typically instantiated once for the complete editor lifecycle
	 * @param dmd the underlying dmd (frame data)
	 * @param dmdWidget the widget
	 * @param display swt display instance (could maybe also created via internal factory)
	 */
	public ClipboardHandler(DMD dmd, DMDWidget dmdWidget, Palette pal) {
		super();
		this.dmd = dmd;
		this.dmdWidget = dmdWidget;
		this.clipboard = new SWTClipboard();
		this.width = dmd.getWidth();
		this.height = dmd.getHeight();
		this.palette = pal;
	}
	
	public void setPalette(Palette p) {
		this.palette = p;
	}

	/**
	 * paste into current image but hoover over to be able to place inserted image
	 */
	public void onPasteHoover() {
		Frame frame = (Frame) clipboard.getContents("DmdFrameTransfer");
		dmdWidget.resetSelection();
		if( frame != null ) {
			if (dmdWidget.isShowMask() && !dmd.getFrame().mask.locked) {
				dmd.addUndoBuffer();
				//ImageData imageData = (ImageData) clipboard.getContents("ImageTransfer");
				//System.arraycopy(imageData.data, 0, dmd.getFrame().mask.data, 0, dmd.getPlaneSize());
				frame.copyToWithMask(dmd.getFrame(), 0b0001);
			} else {
				log.debug("dx={}, dy={}", dx, dy);
				PasteTool pasteTool = new PasteTool(0, width, height,dx,dy);
				pasteTool.setFrameToPaste(frame);
				pasteTool.setMaskOnly(dmdWidget.isShowMask());
				dmdWidget.setDrawTool(pasteTool);
			}
		} else {
			ImageData imageData = (ImageData) clipboard.getContents("ImageTransfer");
			if( imageData != null ) {
				dmd.addUndoBuffer();
				BufferedImage bufferedImage = ImageUtil.convert(new Image(Display.getCurrent(),imageData));
				log.info("pasting image from clipboard: {}, hasAlpha: {}", bufferedImage, bufferedImage.getColorModel().hasAlpha() );
				log.info("target frame no of planes: {}", dmd.getNumberOfPlanes());
				if( dmd.getNumberOfPlanes() <= 6) {
					Frame res = ImageUtil.convertToFrameWithPalette(bufferedImage, dmd, palette, false);
					PasteTool pasteTool = new PasteTool(0, width, height,0,0);
					pasteTool.setFrameToPaste(res);
					pasteTool.setMaskOnly(dmdWidget.isShowMask());
					dmdWidget.setDrawTool(pasteTool);
				} else {
					Frame res = ImageUtil.convertToFrame(bufferedImage, width, height, 8);
					PasteTool pasteTool = new PasteTool(0, width, height,0,0);
					pasteTool.setFrameToPaste(res);
					pasteTool.setMaskOnly(dmdWidget.isShowMask());
					dmdWidget.setDrawTool(pasteTool);
				}
			}
		}
	}
	
	private ImageData buildImageData(DMD dmd, boolean mask, Palette actPalette, Rect sel) {
		ImageData imageData = null;
		if( mask ) {
			PaletteData maskPaletteData = new PaletteData(new RGB[] {new RGB(255,255,255), new RGB(0,0,0)});
			imageData = new ImageData(width, height, 1, maskPaletteData);
			for( int x = 0; x < width; x++) {
				for( int y = 0; y < height; y++ ) {
					if( Rect.selected(sel, x, y) ) {
						imageData.setPixel(x, y, dmd.getPixel(x, y));
					}
				}
			}
		} else {
			if( dmd.getNumberOfPlanes() == 24) {
				imageData = new ImageData(width, height, 24, new PaletteData(0xFF , 0xFF00 , 0xFF0000));
				for( int x = 0; x < width; x++) {
					for( int y = 0; y < height; y++ ) {
						if( Rect.selected(sel, x, y) ) {
							int rgb = dmd.getPixel(x, y);
							imageData.setPixel(x, y, rgb);
						}
					}
				}
			} else if( dmd.getNumberOfPlanes() > 8) {
				// for 32k color
				imageData = new ImageData(width, height, 24, new PaletteData(0xFF , 0xFF00 , 0xFF0000));
				for( int x = 0; x < width; x++) {
					for( int y = 0; y < height; y++ ) {
						if( Rect.selected(sel, x, y) ) {
							int rgb = dmd.getPixel(x, y);
							int r = rgb >> 10;
							int g = (rgb >> 5 ) & 0b011111;
							int b = rgb & 0b011111;
							imageData.setPixel(x, y, imageData.palette.getPixel(new RGB(r<<3,g<<3,b<<3)));
						}
					}
				}
			} else {
				imageData = new ImageData(width, height, 8, buildPaletteData(actPalette));
				for( int x = 0; x < width; x++) {
					for( int y = 0; y < height; y++ ) {
						if( Rect.selected(sel, x, y) ) { 
							imageData.setPixel(x, y, dmd.getPixel(x, y));
						}
					}
				}				
			}
		}
		return imageData;
	}

	/**
	 * build palette data for swt image data based on given palette
	 * @param actPalette color palette from editor
	 * @return
	 */
	private PaletteData buildPaletteData(Palette actPalette) {
		RGB[] rgb = new RGB[actPalette.numberOfColors];
		int i = 0;
		for( com.rinke.solutions.pinball.model.RGB col : actPalette.colors ) {
			rgb[i++] = new RGB(col.red, col.green, col.blue);
		}
		return new PaletteData(rgb);
	}

	public void onCut(Palette pal) {
		//warnNYI();
		onCopy(pal);
		dmd.addUndoBuffer();
		Rect sel = dmdWidget.getSelection();
		// Convenience
		if( sel == null ) sel = new Rect(0, 0, width, height);
		// draw black over area (cut)
		for( int x= 0; x < width; x++) {
			for( int y = 0; y < height; y++) {
				if( Rect.selected(sel, x, y) ) {
					dmd.setPixel(x, y, 0);	// color 0 is always background
				}
			}
		}
		dmdWidget.redraw();
	}

	public void onCopy(Palette activePalette) {
		Rect sel = dmdWidget.getSelection();
		// Convenience
		this.width = dmd.getWidth();
		this.height = dmd.getHeight();
		if( sel == null ) sel = new Rect(0, 0, width, height);
		clipboard.setContents(
			new Object[] { 
					buildImageData(dmd, dmdWidget.isShowMask(), activePalette, sel), 
					buildFrame(dmd, dmdWidget.isShowMask(), dmdWidget.getSelection() ) },
			new String[]{ "ImageTransfer", "DmdFrameTransfer" });
		dx = sel.x1;
		dy = sel.y1;
		dx2 = sel.x2;
		dy2 = sel.y2;
	}

	private Frame buildFrame(DMD dmd, boolean showMask, Rect sel) {
		Frame frame = dmd.getFrame();
		if( showMask ) return frame;
		// if not copying mask, create a mask by using black (color 0) as 'background'
		Frame res = new Frame(frame);
		int len = frame.planes.get(0).data.length;
		byte[] mask = new byte[len];
		Arrays.fill(mask, (byte)0x00);
		for( int i = 0; i < len; i++) {
			for( int j = 0; j < res.planes.size(); j++) {
				mask[i] |= res.planes.get(j).data[i];
			}
		}
		// mask out selection
		if( sel != null) {
			log.debug("masking out {}", sel);
			for( int x= 0; x < width; x++) {
				byte mask1 = (byte) (0b10000000 >> (x % 8));
				for( int y = 0; y < height; y++) {
					if(!sel.inSelection(x, y))
						mask[y*dmd.getBytesPerRow()+x/8] &= ~mask1;
				}
			}
		}
		res.setMask(new Mask(mask,false));
		//ImageUtil.dumpPlane(mask, dmd.getBytesPerRow());
		return res;
	}

	public void onPaste(Boolean scale) {
		for( String item : clipboard.getAvailableTypeNames() ) {
			log.info("Clipboard type: {}", item);
		}
		dmdWidget.resetSelection();
		Frame frame = (Frame) clipboard.getContents("DmdFrameTransfer");
		if( frame != null ) {
			dmd.addUndoBuffer();
			if (dmdWidget.isShowMask() && !dmd.getFrame().mask.locked) {
				ImageData imageData = (ImageData) clipboard.getContents("ImageTransfer");
				System.arraycopy(imageData.data, 0, dmd.getFrame().mask.data, 0, dmd.getPlaneSize());
				//frame.copyToWithMask(dmd.getFrame(), 0b0001);
			} else {
				if (dx != 0 || dy != 0 || dx2 != width || dy2 !=  height || width != dmd.getWidth() || height != dmd.getHeight()) {
					// add selection paste here
					PasteTool pasteTool = new PasteTool(0, width, height,dx,dy);
					pasteTool.setFrameToPaste(frame);
					pasteTool.setMaskOnly(dmdWidget.isShowMask());
					DrawTool lastTool = dmdWidget.getDrawTool();
					dmdWidget.setDrawTool(pasteTool);
					pasteTool.pastePos(dx, dy);
					dmdWidget.setDrawTool(lastTool);
					dmdWidget.resetSelection();
					
				} else {
			    	if (dmd.getFrame().planes.size() == 24 && frame.planes.size() == 24) {
			    		for( int i = 0; i < frame.planes.size(); i++) {
							int size = frame.planes.get(i).data.length;
							System.arraycopy( frame.planes.get(i).data, 0, dmd.getFrame().planes.get(i).data, 0, size);
			    		}
			    	} else {
			    		frame.copyToWithMask(dmd.getFrame(), dmd.getDrawMask());
					}
		    	}
			}
		} else {
			ImageData imageData = (ImageData) clipboard.getContents("ImageTransfer");
			if( imageData != null ) {
				dmd.addUndoBuffer();
				log.info("image data depth: {}", imageData.depth);
				// we need a config option to define if colors are reduced to palette or not
				BufferedImage bufferedImage = ImageUtil.convert(new Image(Display.getCurrent(),imageData));
				if( dmd.getNumberOfPlanes() <= 6) {
					ImageUtil.convertToFrameWithPalette(bufferedImage, dmd, palette, true);
					//dmd.setFrame(res);
				} else {
					Frame res = ImageUtil.convertToFrame(bufferedImage, dmd.getWidth(), dmd.getHeight(), 8, scale);
					dmd.setFrame(res);
				}
			}
		}
	}

	public void onReplace() {
		for( String item : clipboard.getAvailableTypeNames() ) {
			log.info("Clipboard type: {}", item);
		}
		dmdWidget.resetSelection();
		Frame frame = (Frame) clipboard.getContents("DmdFrameTransfer");
		if( frame != null ) {
			dmd.addUndoBuffer();
			frame.copyToWithMask(dmd.getFrame(), Constants.DEFAULT_DRAW_MASK);
		} 
	}
	
}


