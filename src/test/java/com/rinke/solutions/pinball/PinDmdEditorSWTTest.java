package com.rinke.solutions.pinball;

import static com.fappel.swt.SWTEventHelper.trigger;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.fappel.swt.DisplayHelper;

@RunWith(MockitoJUnitRunner.class)
public class PinDmdEditorSWTTest {
	
	@InjectMocks
	private PinDmdEditor pinDmdEditor = new PinDmdEditor();
	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	@Test
	public void testSetDuration() {
		pinDmdEditor.animations = new ArrayList<>();
		
		Shell shell = displayHelper.createShell();
		pinDmdEditor.createContents(shell);
		trigger(SWT.Selection).on(pinDmdEditor.btnSetDuration);
		
//		//assertEquals(1, counter.getCount());
	}

}
