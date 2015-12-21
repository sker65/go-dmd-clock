package com.rinke.solutions.pinball;

import static com.fappel.swt.SWTEventHelper.trigger;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.fappel.swt.DisplayHelper;


@RunWith(MockitoJUnitRunner.class)
public class PinDmdEditorSWTTest {
	
	//@InjectMocks
	private PinDmdEditor uut = new PinDmdEditor();
	
	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	@Before
	public void setup() {
		Shell shell = displayHelper.createShell();
		uut.createContents(shell);	
		uut.createNewProject();
		
		uut.animationHandler = new  AnimationHandler(null,null,null,null,false);
		
		uut.createBindings();
	}
	
	@Test
	public void testCreateNewPalette() {
		assertThat(uut.activePalette, notNullValue());
		
		trigger(SWT.Selection).on(uut.btnNewPalette);
		assertThat(uut.activePalette, notNullValue());
		assertThat(uut.project.palettes.size(), equalTo(2));
	}
	
	@Test
	public void testRenamePalette() {
		assertThat(uut.activePalette, notNullValue());
		assertThat(uut.activePalette.name, equalTo("default"));
		
		uut.paletteComboViewer.getCombo().setText("2 - foo");
		trigger(SWT.Selection).on(uut.btnRenamePalette);
		assertThat(uut.activePalette, notNullValue());
		assertThat(uut.activePalette.name, equalTo("foo"));
	}
	
	@Test
	public void testAniStop() {
		uut.animationHandler.stop();
		assertThat(uut.toolBar.getEnabled(), equalTo(true));
	}

	@Test
	public void testAniStart() {
		uut.animationHandler.start();
		assertThat(uut.toolBar.getEnabled(), equalTo(false));
	}

}
