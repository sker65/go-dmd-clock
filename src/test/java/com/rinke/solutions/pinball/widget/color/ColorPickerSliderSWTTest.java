package com.rinke.solutions.pinball.widget.color;

import java.awt.image.BufferedImage;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fappel.swt.DisplayHelper;

@RunWith(MockitoJUnitRunner.class)
public class ColorPickerSliderSWTTest {

	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();
	
	@Mock
	ColorPicker cp;

	ColorPickerSlider uut;

	private Shell shell;

	private Display display; 

	@Before
	public void setUp() throws Exception {
		display = displayHelper.getDisplay();
		shell = displayHelper.createShell();
		uut = new ColorPickerSlider(shell,cp,0);
		//uut.
	}
	
	@Test
	public void testPaintTrack() throws Exception {
		Image img = new Image(display,200,100);
		GC gc = new GC(img); 
		uut.paintTrack(gc, display);
	}

	@Test
	public void testConvertToSWT() throws Exception {
		BufferedImage bufferedImage = new BufferedImage(100,100,BufferedImage.TYPE_4BYTE_ABGR);
		uut.convertToSWT(bufferedImage);
	}
}
