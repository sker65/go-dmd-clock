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
public class ClipboardHandler {
	
	DMD dmd;
	Clipboard clipboard;
	DMDWidget dmdWidget;
	int width;
	int height;
	private int planeSize;
	
	/**
	 * typically instantiated once for the complete editor lifecycle
	 * @param dmd the underlying dmd (frame data)
	 * @param dmdWidget the widget
	 * @param display swt display instance (could maybe also created via internal factory)
	 */
	public ClipboardHandler(DMD dmd, DMDWidget dmdWidget) {
		super();
		this.dmd = dmd;
		this.dmdWidget = dmdWidget;
		Display display = Display.getCurrent();
		this.clipboard = new Clipboard(display);
		this.width = dmd.getWidth();
		this.height = dmd.getHeight();
		this.planeSize = dmd.getPlaneSizeInByte();
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
		}
		// TODO build frame from image data
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
			new Object[] { buildImageData(dmd, dmdWidget.isShowMask(), activePalette), dmd.getFrame() },
			new Transfer[]{ ImageTransfer.getInstance(), DmdFrameTransfer.getInstance() });
	}

	public void onPaste() {
		for( String item : clipboard.getAvailableTypeNames() ) {
			log.info("Clipboard type: {}", item);
		}
		Frame frame = (Frame) clipboard.getContents(DmdFrameTransfer.getInstance());
		if( frame != null ) {
			if (dmdWidget.isShowMask()) {
				dmd.addUndoBuffer();
				dmd.getFrame().setMask(Arrays.copyOf(frame.planes.get(0).plane, planeSize));
			} else {
				dmd.addUndoBuffer();
				int mask = dmd.getDrawMask();
				Frame f = dmd.getFrame();
				for( int j = 0; j < f.planes.size(); j++) {
					if (((1 << j) & mask) != 0) {
						System.arraycopy(frame.planes.get(j).plane, 0, f.planes.get(j).plane, 0, planeSize);
					}
				}
			}
		} else {
			ImageData imageData = (ImageData) clipboard.getContents(ImageTransfer.getInstance());
			if( imageData != null ) {
				dmd.addUndoBuffer();
				log.info("image data depth: {}", imageData.depth);
				// we need a config option to define if colors are reduced to palette or not
				BufferedImage bufferedImage = ImageUtil.convert(new Image(Display.getCurrent(),imageData));
				Frame res = ImageUtil.convertToFrame(bufferedImage, width, height);
				dmd.setFrame(res);
			}
		}
	}
}
