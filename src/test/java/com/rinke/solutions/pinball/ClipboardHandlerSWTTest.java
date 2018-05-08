package com.rinke.solutions.pinball;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.widget.DMDWidget;
import com.rinke.solutions.pinball.widget.DMDWidget.Rect;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClipboardHandlerSWTTest {
	
	@Mock
	private ClipboardFacade clipboardMock;

	private DMD dmd = new DMD(128, 32);

	@Mock
	private DMDWidget dmdWidget;
	
	@Mock Palette pal;
	
	@InjectMocks
	private ClipboardHandler clipboardHandler = new ClipboardHandler(dmd, dmdWidget, pal);
	
	@Before
	public void setup() {
		clipboardHandler.width = 128;
		clipboardHandler.height = 32;
		clipboardHandler.clipboard = clipboardMock;
	}

	@Test
	public void testOnPasteHoover() throws Exception {
		clipboardHandler.onPasteHoover();
	}

	@Test
	public void testOnPasteHooverWithFrame() throws Exception {
		Frame frame = new Frame();
		when(clipboardMock.getContents(eq("DmdFrameTransfer"))).thenReturn(frame);
		clipboardHandler.onPasteHoover();
	}
	
	org.eclipse.swt.graphics.RGB[] getColors() {
		org.eclipse.swt.graphics.RGB[] cols = new org.eclipse.swt.graphics.RGB[2];
		cols[0] = new org.eclipse.swt.graphics.RGB(0,0,0);
		cols[0] = new org.eclipse.swt.graphics.RGB(255,0,0);
		return cols;
	}
	
	@Test
	public void testOnPasteHooverWithImageTransfer() throws Exception {
		PaletteData palette = new PaletteData(getColors());
		ImageData imageData = new ImageData(128, 32, 8, palette);
		when(clipboardMock.getContents(eq("ImageTransfer"))).thenReturn(imageData);
		
		clipboardHandler.palette = Palette.getDefaultPalettes().get(0);
		clipboardHandler.onPasteHoover();
	}
	
	@Test
	public void testOnPasteHooverWithImageTransferMorePlane() throws Exception {
		PaletteData palette = new PaletteData(getColors());
		ImageData imageData = new ImageData(128, 32, 8, palette);
		when(clipboardMock.getContents(eq("ImageTransfer"))).thenReturn(imageData);
		
		dmd.setNumberOfPlanes(15);
		
		clipboardHandler.palette = Palette.getDefaultPalettes().get(0);
		clipboardHandler.onPasteHoover();
	}

	Palette getPalette() {
		RGB[] colors = new RGB[1];
		colors[0] = new RGB(1, 2, 3);
		return new Palette(colors );
	}
	
	@Test
	public void testOnCut() throws Exception {
		Palette activePalette = getPalette();
		clipboardHandler.onCut(activePalette );
	}
	
	@Test
	public void testOnCopy() throws Exception {
		Palette activePalette = getPalette();
		clipboardHandler.onCopy(activePalette );
		//verify(clipboardMock).setContents(any(Object[].class), any(Transfer[].class));
	}

	@Test
	public void testOnCopyWithMask() throws Exception {
		Palette activePalette = getPalette();
		when(dmdWidget.isShowMask()).thenReturn(true);
		clipboardHandler.onCopy(activePalette );
		//verify(clipboardMock).setContents(any(Object[].class), any(Transfer[].class));
	}

	@Test
	public void testOnCopyWithHiColor() throws Exception {
		Palette activePalette = getPalette();
		when(dmdWidget.isShowMask()).thenReturn(false);
		dmd.setNumberOfPlanes(15);
		clipboardHandler.onCopy(activePalette );
		//verify(clipboardMock).setContents(any(Object[].class), any(Transfer[].class));
	}
	
	@Test
	public void testOnCopyWithSelection() throws Exception {
		Palette activePalette = getPalette();
		when(dmdWidget.isShowMask()).thenReturn(false);
		when(dmdWidget.getSelection()).thenReturn(new Rect(0, 0, 100, 20));
		dmd.setNumberOfPlanes(15);
		clipboardHandler.onCopy(activePalette );
		//verify(clipboardMock).setContents(any(Object[].class), any(Transfer[].class));
	}
	
	@Test
	public void testOnPaste() throws Exception {
		Object obj = new Frame();
		when(clipboardMock.getContents(eq("DmdFrameTransfer"))).thenReturn(obj);
		when(clipboardMock.getAvailableTypeNames()).thenReturn(new String[]{"foo"});
		clipboardHandler.onPaste();
	}

	@Test
	public void testOnPasteWithImageTransfer() throws Exception {
		PaletteData palette = new PaletteData(getColors());
		ImageData imageData = new ImageData(128, 32, 8, palette);
		when(clipboardMock.getContents(eq("ImageTransfer"))).thenReturn(imageData);
		when(clipboardMock.getAvailableTypeNames()).thenReturn(new String[]{"foo"});
		clipboardHandler.palette = Palette.getDefaultPalettes().get(0);
		clipboardHandler.onPaste();
	}
}
