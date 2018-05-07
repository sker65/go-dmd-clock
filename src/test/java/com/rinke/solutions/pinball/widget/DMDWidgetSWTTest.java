package com.rinke.solutions.pinball.widget;

import static org.junit.Assert.*;

import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.fappel.swt.DisplayHelper;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.widget.DMDWidget.Rect;

public class DMDWidgetSWTTest {

	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	Shell shell;

	private DMDWidget dmdWidget;

	private DMD dmd;
	DmdSize size = DmdSize.Size128x32;

	@Before
	public void setUp() throws Exception {
		shell = displayHelper.createShell();
		dmd = new DMD(size);
		dmdWidget = new DMDWidget(shell, 0, dmd , false);
		dmdWidget.setPalette(Palette.getDefaultPalettes().get(0));
	}
	
	@Test
	public void testSetSelection() {
		dmdWidget.setSelection(new Rect(0,0,10,10));
	}
	
	@Test
	public void testRectIntersect() {
		Rect r = new Rect(0, 0, 10, 10);
		Rect r2 = new Rect(0, 0, 10, 10);
		Rect r3 = new Rect(0, 0, 11, 10);
		assertTrue( r.equals(r2));
		assertFalse( r.equals(r3));
		assertTrue( r.inSelection(2, 4));
		assertFalse( r.inSelection(2, 14));
		assertTrue( r.isOnSelectionMark(0, 10));
		assertFalse( r.isOnSelectionMark(1, 110));
	}

	@Test
	public void testSetDrawTool() {
		dmdWidget.setDrawTool(new LineTool(0));
		dmdWidget.setDrawTool(null);
		dmdWidget.setDrawTool(new SelectTool(0));
	}
	
	@Test
	public void testSetSelectionWithNull() {
		dmdWidget.setSelection(null);
	}
	
	@Test
	public void testSetShowMask() {
		dmdWidget.setShowMask(true);
		dmdWidget.setShowMask(false);
	}
	
	@Test
	public void testSetMask() {
		Mask mask = new Mask(512);
		dmdWidget.setMask(mask );
	}
	
	@Test
	public void testTransformColor( ) {
		com.rinke.solutions.pinball.model.RGB rgb = new com.rinke.solutions.pinball.model.RGB(128, 128, 128);
		dmdWidget.transformColor(rgb, false);
	}


	@Test
	public void testSetBounds() throws Exception {
		dmdWidget.setBounds(0, 0, size.width*4, size.height*4);
	}

	@Test
	public void testDrawImage() throws Exception {
		dmdWidget.drawImage(displayHelper.getDisplay(), size.width, size.height);
	}

	@Test
	public void testDrawImageWithSelection() throws Exception {
		dmdWidget.setSelection(new Rect(0,20,30,35));
		dmdWidget.drawImage(displayHelper.getDisplay(), size.width, size.height);
	}

	@Test
	public void testDrawImageWithMask() throws Exception {
		Mask mask = new Mask(size.planeSize);
		dmd.setMask(mask.data);
		dmdWidget.setMask(mask);
		dmdWidget.setShowMask(true);
		dmdWidget.setMaskOut(true);
		dmdWidget.setMaskLocked(true);
		dmdWidget.drawImage(displayHelper.getDisplay(), size.width, size.height);
	}
}
