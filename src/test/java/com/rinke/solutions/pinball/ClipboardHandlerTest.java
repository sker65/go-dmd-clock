package com.rinke.solutions.pinball;

import org.eclipse.swt.dnd.Clipboard;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.widget.DMDWidget;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class ClipboardHandlerTest {
	@Mock
	private Clipboard clipboard;

	@Mock
	private DMD dmd;

	@Mock
	private DMDWidget dmdWidget;

	//@Mock
	private Palette palette;

	private Frame frame;
	
	@Before public void setup() {
		palette = getPalette();
		when(dmd.getNumberOfPlanes()).thenReturn(2);
		frame = new Frame(new byte[512], new byte[512]);
		when(dmd.getFrame()).thenReturn(frame);
		clipboardHandler.width = 128;
		clipboardHandler.height = 32;
	}
	
	@InjectMocks
	private ClipboardHandler clipboardHandler;

	@Test
	public void testOnPasteHoover() throws Exception {
		clipboardHandler.onPasteHoover();
	}

	@Test
	public void testOnCut() throws Exception {
		clipboardHandler.onCut(palette);
	}

	@Test
	public void testOnCopy() throws Exception {
		clipboardHandler.onCopy(palette);
	}

	private Palette getPalette() {
		return new Palette( Palette.defaultColors() );
	}

	@Test
	public void testOnPaste() throws Exception {
		clipboardHandler.onPaste();
	}

}
