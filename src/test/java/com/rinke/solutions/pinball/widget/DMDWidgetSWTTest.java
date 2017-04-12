package com.rinke.solutions.pinball.widget;

import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.fappel.swt.DisplayHelper;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Palette;

public class DMDWidgetSWTTest {

	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	Shell shell;

	private DMDWidget dmdWidget;

	private DMD dmd;

	@Before
	public void setUp() throws Exception {
		shell = displayHelper.createShell();
		dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		dmdWidget = new DMDWidget(shell, 0, dmd , false);
		dmdWidget.setPalette(Palette.getDefaultPalettes().get(0));
	}
	
	@Test
	public void testTransformColor( ) {
		com.rinke.solutions.pinball.model.RGB rgb = new com.rinke.solutions.pinball.model.RGB(128, 128, 128);
		dmdWidget.transformColor(rgb, false);
	}


	@Test
	public void testSetBounds() throws Exception {
		dmdWidget.setBounds(0, 0, PinDmdEditor.DMD_WIDTH*4, PinDmdEditor.DMD_HEIGHT*4);
	}

	@Test
	public void testDrawImage() throws Exception {
		dmdWidget.drawImage(displayHelper.getDisplay(), PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
	}

	@Test
	public void testDrawImageWithMask() throws Exception {
		DMD mask = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		dmdWidget.setMaskLocked(true);
		dmdWidget.drawImage(displayHelper.getDisplay(), PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
	}
}
