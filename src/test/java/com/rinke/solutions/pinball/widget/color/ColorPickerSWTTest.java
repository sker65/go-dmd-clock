package com.rinke.solutions.pinball.widget.color;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.fappel.swt.DisplayHelper;

public class ColorPickerSWTTest {

	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();
	
	ColorPicker uut;

	private Shell shell;

	private Display display; 

	@Before
	public void setUp() throws Exception {
		display = displayHelper.getDisplay();
		shell = displayHelper.createShell();
		uut = new ColorPicker(display, shell);
		uut.createContents();
	}
	
	@Test
	public void testSetRGB() throws Exception {
		uut.setRGB(0, 0, 0);
	}

	@Test
	public void testSetHSB() throws Exception {
		uut.setHSB(120.0f, 0.5f, 0.5f);
	}
}
