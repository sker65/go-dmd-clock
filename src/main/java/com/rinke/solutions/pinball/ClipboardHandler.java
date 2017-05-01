package com.rinke.solutions.pinball;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.renderer.ImageUtil;
import com.rinke.solutions.pinball.widget.DMDWidget;
import com.rinke.solutions.pinball.widget.PasteTool;

/**
 * handler class for clipboard actions in the editor (the dmd preview widget). handles copy & paste actions
 * @author Stefan Rinke
 */
@Slf4j
public class ClipboardHandler implements Runnable {
	
	DMD dmd;
	Clipboard clipboard;
	DMDWidget dmdWidget;
	int width;
	int height;
	Palette palette;
	Display display;
	
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
		this.display = Display.getCurrent();
		this.clipboard = new Clipboard(display);
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
		Frame frame = (Frame) clipboard.getContents(DmdFrameTransfer.getInstance());
		if( frame != null ) {
			PasteTool pasteTool = new PasteTool(0, width, height);
			pasteTool.setFrameToPaste(frame);
			pasteTool.setMaskOnly(dmdWidget.isShowMask());
			dmdWidget.setDrawTool(pasteTool);
		} else {
			ImageData imageData = (ImageData) clipboard.getContents(ImageTransfer.getInstance());
			if( imageData != null ) {
				dmd.addUndoBuffer();
				BufferedImage bufferedImage = ImageUtil.convert(new Image(Display.getCurrent(),imageData));
				if( dmd.getNumberOfPlanes() <= 4) {
					Frame res = ImageUtil.convertToFrameWithPalette(bufferedImage, dmd, palette, false);
					PasteTool pasteTool = new PasteTool(0, width, height);
					pasteTool.setFrameToPaste(res);
					pasteTool.setMaskOnly(dmdWidget.isShowMask());
					dmdWidget.setDrawTool(pasteTool);
				} else {
					Frame res = ImageUtil.convertToFrame(bufferedImage, width, height);
					PasteTool pasteTool = new PasteTool(0, width, height);
					pasteTool.setFrameToPaste(res);
					pasteTool.setMaskOnly(dmdWidget.isShowMask());
					dmdWidget.setDrawTool(pasteTool);
				}
			}
		}
	}
	
	private ImageData buildImageData(DMD dmd, boolean mask, Palette actPalette) {
		ImageData imageData = null;
		if( mask ) {
			PaletteData maskPaletteData = new PaletteData(new RGB[] {new RGB(255,255,255), new RGB(0,0,0)});
			imageData = new ImageData(width, height, 1, maskPaletteData);
			for( int x = 0; x < width; x++) {
				for( int y = 0; y < height; y++ ) {
					imageData.setPixel(x, y, dmd.getPixel(x, y));
				}
			}
		} else {
			if( dmd.getNumberOfPlanes() > 5) {
				// for 32k color
				imageData = new ImageData(width, height, 24, new PaletteData(0xFF , 0xFF00 , 0xFF0000));
				for( int x = 0; x < width; x++) {
					for( int y = 0; y < height; y++ ) {
						int rgb = dmd.getPixel(x, y);
						int r = rgb >> 10;
						int g = (rgb >> 5 ) & 0b011111;
						int b = rgb & 0b011111;
						imageData.setPixel(x, y, imageData.palette.getPixel(new RGB(r<<3,g<<3,b<<3)));
					}
				}
			} else {
				imageData = new ImageData(width, height, dmd.getNumberOfPlanes(), buildPaletteData(actPalette));
				for( int x = 0; x < width; x++) {
					for( int y = 0; y < height; y++ ) {
						imageData.setPixel(x, y, dmd.getPixel(x, y));
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

	public void onCopy(Palette activePalette) {
		clipboard.setContents(
			new Object[] { buildImageData(dmd, dmdWidget.isShowMask(), activePalette), buildFrame(dmd.getFrame(), dmdWidget.isShowMask()) },
			new Transfer[]{ ImageTransfer.getInstance(), DmdFrameTransfer.getInstance() });
	}

	private Frame buildFrame(Frame frame, boolean showMask) {
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
		res.setMask(mask);
		return res;
	}

	public void onPaste() {
		for( String item : clipboard.getAvailableTypeNames() ) {
			log.info("Clipboard type: {}", item);
		}
		Frame frame = (Frame) clipboard.getContents(DmdFrameTransfer.getInstance());
		if( frame != null ) {
			dmd.addUndoBuffer();
			if (dmdWidget.isShowMask()) {
				frame.copyToWithMask(dmd.getFrame(), 0b0001);
			} else {
				frame.copyToWithMask(dmd.getFrame(), dmd.getDrawMask());
			}
		} else {
			ImageData imageData = (ImageData) clipboard.getContents(ImageTransfer.getInstance());
			if( imageData != null ) {
				dmd.addUndoBuffer();
				log.info("image data depth: {}", imageData.depth);
				// we need a config option to define if colors are reduced to palette or not
				BufferedImage bufferedImage = ImageUtil.convert(new Image(Display.getCurrent(),imageData));
				if( dmd.getNumberOfPlanes() <= 4) {
					ImageUtil.convertToFrameWithPalette(bufferedImage, dmd, palette, true);
					//dmd.setFrame(res);
				} else {
					Frame res = ImageUtil.convertToFrame(bufferedImage, width, height);
					dmd.setFrame(res);
				}
			}
		}
	}
	
	long nextCheck = 0;

	@Override
	public void run() {
		long now = System.currentTimeMillis();
		if( now > nextCheck ) {
			nextCheck = now + 10000;
			checkClipboard();
		}
		display.asyncExec(this);
	}

	private void checkClipboard() {
		log.trace("check clipboard");
	}
}
