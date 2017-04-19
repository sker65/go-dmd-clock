package com.rinke.solutions.pinball;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClipboardHandlerSWTTest {
	
	@Mock
	private Clipboard clipboardMock;

	@Mock
	private DMD dmd;

	@Mock
	private DMDWidget dmdWidget;
	
	@InjectMocks
	private ClipboardHandler clipboardHandler;
	
	@Before
	public void setup() {
		when(dmd.getNumberOfPlanes()).thenReturn(4);
		when(dmd.getWidth()).thenReturn(128);
		when(dmd.getHeight()).thenReturn(32);
		clipboardHandler.width = 128;
		clipboardHandler.height = 32;
		clipboardHandler.clipboard = clipboardMock;
	}

	@Test
	public void testOnPasteHoover() throws Exception {
		clipboardHandler.onPasteHoover();
	}

	@Test
	public void testOnCopy() throws Exception {
		RGB[] colors = new RGB[1];
		colors[0] = new RGB(1, 2, 3);
		Palette activePalette = new Palette(colors );
		clipboardHandler.onCopy(activePalette );
		//verify(clipboardMock).setContents(any(Object[].class), any(Transfer[].class));
	}

	@Test
	public void testOnPaste() throws Exception {
		Object obj = new Frame();
		when(clipboardMock.getContents(eq(DmdFrameTransfer.getInstance()))).thenReturn(obj);
		when(clipboardMock.getAvailableTypeNames()).thenReturn(new String[]{"foo"});
		Frame dmdFrame = new Frame();
		when(dmd.getFrame()).thenReturn(dmdFrame);
		clipboardHandler.onPaste();
	}

}
