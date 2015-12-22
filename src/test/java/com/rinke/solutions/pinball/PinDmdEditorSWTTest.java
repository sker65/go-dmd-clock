package com.rinke.solutions.pinball;

import static com.fappel.swt.SWTEventHelper.trigger;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.fappel.swt.DisplayHelper;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;


@RunWith(MockitoJUnitRunner.class)
public class PinDmdEditorSWTTest {
	
	//@InjectMocks
	private PinDmdEditor uut = new PinDmdEditor();
	
	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	Shell shell;
	
	@Before
	public void setup() {
		shell = displayHelper.createShell();
		uut.createContents(shell);	
		uut.createNewProject();
		
		uut.animationHandler = new  AnimationHandler(null,null,null,null,false);
		
		uut.createBindings();
		
		byte[] digest = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
		uut.hashes.add(digest);

	}
	
	@Test
	public void testLoadProjectString() throws Exception {
		uut.shell = shell;
		uut.loadProject("./src/test/resources/test.xml");
	}

	
	@Test
	public void testCreateNewPalette() {
		assertThat(uut.activePalette, notNullValue());
		
		trigger(SWT.Selection).on(uut.btnNewPalette);
		assertThat(uut.activePalette, notNullValue());
		assertThat(uut.project.palettes.size(), equalTo(2));
		
		// test that new palette is selected
		Palette palette = uut.project.palettes.get(1);
		Object element = ((StructuredSelection)uut.paletteComboViewer.getSelection()).getFirstElement();
		assertThat(palette,equalTo(element));
	}
	@Test
	public void testOnlyDefaultPalette() {
		assertThat(uut.activePalette, notNullValue());
		assertThat(uut.project.palettes.size(), equalTo(1));
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
	
	@Test
	public void testAddPalSwitch() {
		trigger(SWT.Selection).on(uut.btnAddKeyframe);
		assertThat(uut.project.palMappings.size(), equalTo(1));
		assertThat(uut.project.palMappings.get(0).name, equalTo("KeyFrame 1"));
	}

	@Test
	public void testAddFrameSeq() {
		
		Animation animation = new Animation(AnimationType.PNG, "test", 0, 0, 0, 0, 0);
		animation.setDesc("foo");
		animation.setLoadedFromFile(false);
		uut.animations.put("foo", animation );

		// frameSeqView must have a selection
		uut.buildFrameSeqList();
		uut.frameSeqViewer.setSelection(new StructuredSelection(uut.frameSeqList.get(0)), true);
		
		trigger(SWT.Selection).on(uut.btnAddFrameSeq);
		assertThat(uut.project.palMappings.size(), equalTo(1));
		PalMapping mapping = uut.project.palMappings.get(0);
		assertThat(mapping.name, equalTo("KeyFrame foo"));
		assertThat(mapping.digest[0], equalTo((byte)1));
		assertThat(mapping.frameSeqName, equalTo("foo"));
	}

}
