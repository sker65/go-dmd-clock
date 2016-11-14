package com.rinke.solutions.pinball.widget.color;

import java.awt.image.BufferedImage;

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
public class ColorPickerPanelSWTTest {
	
	@Mock
	ColorPicker cp;
	
	ColorPickerPanel uut;

	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();
	
	private Shell shell;

	private Display display; 


	@Before
	public void setUp() throws Exception {
		display = displayHelper.getDisplay();
		shell = displayHelper.createShell();
		uut = new  ColorPickerPanel(shell, cp, 0);
	}

	@Test
	public void testConvertToSWT() throws Exception {
		BufferedImage bufferedImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		uut.convertToSWT(bufferedImage );
	}

	@Test
	public void testSetMode() throws Exception {
		uut.setMode(ColorPicker.Mode.HUE);
	}

	@Test
	public void testSetRGB() throws Exception {
		uut.setRGB(0, 0, 0);
	}

	@Test
	public void testGetHSB() throws Exception {
	}

	@Test
	public void testGetRGB() throws Exception {
	}

	@Test
	public void testGetHSBPoint() throws Exception {
	}

	@Test
	public void testGetRGBPoint() throws Exception {
	}

	@Test
	public void testSetHSB() throws Exception {
	}

}
