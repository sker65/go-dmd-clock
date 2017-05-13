package com.rinke.solutions.pinball.view.handler;

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

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdFrameTransfer;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.renderer.ImageUtil;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.Rect;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.view.model.ViewModel.PasteData;

@Bean
@Slf4j
public class ClipboardHandler extends ViewHandler {

	DMD dmd;
	Clipboard clipboard;
	Palette palette;
	Display display;

	@Autowired
	DrawingHandler drawingHandler;
	
	public ClipboardHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm,m,d);
		this.display = Display.getCurrent();
		this.clipboard = new Clipboard(display);
		this.dmd = vm.dmd;
	}
	
	/**
	 * paste into current image but hoover over to be able to place inserted image
	 */
	public void onPasteHoover() {
		Frame frame = (Frame) clipboard.getContents(DmdFrameTransfer.getInstance());
		//dmdWidget.resetSelection();
		vm.setDmdSelection(null);
		if( frame != null ) {
			log.debug("dx={}, dy={}", vm.pasteData.dx, vm.pasteData.dy);
			PasteData pasteData = new PasteData(vm.pasteData);
			pasteData.maskOnly = vm.isMaskVisible();
			vm.setPasteData(pasteData);
			vm.setDrawTool("paste");
		} else {
			ImageData imageData = (ImageData) clipboard.getContents(ImageTransfer.getInstance());
			if( imageData != null ) {
				dmd.addUndoBuffer();
				BufferedImage bufferedImage = ImageUtil.convert(new Image(Display.getCurrent(),imageData));
				Frame res = null;
				if( dmd.getNumberOfPlanes() <= 4) {
					res = ImageUtil.convertToFrameWithPalette(bufferedImage, dmd, palette, false);
				} else {
					res = ImageUtil.convertToFrame(bufferedImage, vm.pasteData.width, vm.pasteData.height);
				}
				vm.setPasteData(new PasteData(0,0,vm.pasteData.width,vm.pasteData.height,res,vm.maskVisible));
				vm.setDrawTool("paste");
			}
		}
	}
	
	private ImageData buildImageData(DMD dmd, boolean mask, Palette actPalette, Rect sel) {
		ImageData imageData = null;
		if( mask ) {
			PaletteData maskPaletteData = new PaletteData(new RGB[] {new RGB(255,255,255), new RGB(0,0,0)});
			imageData = new ImageData(vm.pasteData.width, vm.pasteData.height, 1, maskPaletteData);
			for( int x = 0; x < vm.pasteData.width; x++) {
				for( int y = 0; y < vm.pasteData.height; y++ ) {
					if( Rect.selected(sel, x, y) ) {
						imageData.setPixel(x, y, dmd.getPixel(x, y));
					}
				}
			}
		} else {
			if( dmd.getNumberOfPlanes() > 5) {
				// for 32k color
				imageData = new ImageData(vm.pasteData.width, vm.pasteData.height, 24, new PaletteData(0xFF , 0xFF00 , 0xFF0000));
				for( int x = 0; x < vm.pasteData.width; x++) {
					for( int y = 0; y < vm.pasteData.height; y++ ) {
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
				imageData = new ImageData(vm.pasteData.width, vm.pasteData.height, dmd.getNumberOfPlanes(), buildPaletteData(actPalette));
				for( int x = 0; x < vm.pasteData.width; x++) {
					for( int y = 0; y < vm.pasteData.height; y++ ) {
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
		Rect sel = vm.dmdSelection;
		// draw black over area (cut)
		for( int x= 0; x < vm.pasteData.width; x++) {
			for( int y = 0; y < vm.pasteData.height; y++) {
				if( Rect.selected(sel, x, y) ) {
					dmd.setPixel(x, y, 0);	// color 0 is always background
				}
			}
		}
		// TODO done via binding dmdWidget.redraw();
	}

	public void onCopy(Palette activePalette) {
		Rect sel = vm.dmdSelection;
		clipboard.setContents(
			new Object[] { 
					buildImageData(dmd, vm.maskVisible, activePalette, sel), 
					buildFrame(dmd, vm.maskVisible, vm.dmdSelection) },
			new Transfer[]{ ImageTransfer.getInstance(), DmdFrameTransfer.getInstance() });
		PasteData pd = new PasteData();
		if( sel != null ) {
			pd.dx = sel.x1;
			pd.dy = sel.y1;
		} else {
			pd.dy = pd.dx = 0;
		}
		vm.setPasteData(pd);
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
			for( int x= 0; x < vm.pasteData.width; x++) {
				byte mask1 = (byte) (0b10000000 >> (x % 8));
				for( int y = 0; y < vm.pasteData.height; y++) {
					if(!sel.inSelection(x, y))
						mask[y*dmd.getBytesPerRow()+x/8] &= ~mask1;
				}
			}
		}
		res.setMask(mask);
		//ImageUtil.dumpPlane(mask, dmd.getBytesPerRow());
		return res;
	}

	public void onPaste() {
		for( String item : clipboard.getAvailableTypeNames() ) {
			log.info("Clipboard type: {}", item);
		}
		vm.setDmdSelection(null);
		// TODO check binding dmdWidget.resetSelection();
		Frame frame = (Frame) clipboard.getContents(DmdFrameTransfer.getInstance());
		if( frame != null ) {
			dmd.addUndoBuffer();
			if (vm.maskVisible) {
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
					Frame res = ImageUtil.convertToFrame(bufferedImage, vm.pasteData.width, vm.pasteData.height);
					dmd.setFrame(res);
				}
			}
		}
	}
	
	long nextCheck = 0;

	// CMD Handler
	public void onSelectAll() {
		vm.setDmdSelection(new Rect(0, 0, vm.dmd.getWidth(), vm.dmd.getHeight()));
	}

	public void onRemoveSelection() {
		vm.setDmdSelection(null);
	}

}
