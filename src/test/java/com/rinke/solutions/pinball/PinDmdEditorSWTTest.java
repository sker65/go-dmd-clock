package com.rinke.solutions.pinball;

import static com.fappel.swt.SWTEventHelper.trigger;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.rules.Verifier;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.fappel.swt.DisplayHelper;
import com.fappel.swt.JFaceViewerHelper;
import com.fappel.swt.SWTEventHelper;
import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;

import static com.fappel.swt.JFaceViewerHelper.fireSelectionChanged;

@RunWith(MockitoJUnitRunner.class)
public class PinDmdEditorSWTTest {
	
	//@InjectMocks
	private PinDmdEditor uut = new PinDmdEditor();
	
	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	Shell shell;
	
	public EventHandler eventHandler = new EventHandler() {
		
		@Override
		public void notifyAni(AniEvent evt) {
			
		}
	};
	
	@Before
	public void setup() {
		shell = displayHelper.createShell();
		uut.createContents(shell);	
		uut.createNewProject();
		
		DMD dmd = new DMD(128,32);
		uut.animationHandler = new  AnimationHandler(null,null,dmd ,false);
		uut.animationHandler.setScale(uut.scale);
		uut.animationHandler.setEventHandler(eventHandler);
		
		uut.createBindings();
		
		byte[] digest = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
		uut.hashes.add(digest);

	}
	
	@Rule
	public Verifier verifier= new Verifier() {

		@Override
		protected void verify() throws Throwable {
			GlobalExceptionHandler handler = GlobalExceptionHandler.getInstance();
			Exception exception = handler.getLastException();
			if( exception != null ) {
				handler.setLastException(null);
				throw exception;
			}
		}
	      
	};
	
	@Test
	public void testSelectKeyFrame() throws Exception {
		uut.shell = shell;
		PalMapping palMapping = new PalMapping(0,"foo");
		palMapping.animationName = "drwho-dump";
		palMapping.frameIndex = 0;
		
		uut.loadAni("./src/test/resources/drwho-dump.txt.gz", false, true);
		
		SelectionChangedEvent e = new SelectionChangedEvent(uut.keyframeListViewer, 
				new StructuredSelection(palMapping));
		fireSelectionChanged(uut.keyframeListViewer, e);
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
