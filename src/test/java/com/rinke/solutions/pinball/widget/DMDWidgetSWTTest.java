package com.rinke.solutions.pinball.widget;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fappel.swt.DisplayHelper;
import com.rinke.solutions.pinball.DMD;
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
		dmd = new DMD(128, 32);
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
		dmdWidget.setBounds(0, 0, 128*4, 32*4);
	}

	@Test
	public void testDrawImage() throws Exception {
		dmdWidget.drawImage(displayHelper.getDisplay(), 128, 32);
	}

	@Test
	public void testDrawImageWithMask() throws Exception {
		DMD mask = new DMD(128, 32);
		dmdWidget.setMask(mask , true);
		dmdWidget.drawImage(displayHelper.getDisplay(), 128, 32);
	}
}
