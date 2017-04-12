package com.rinke.solutions.pinball;

import static com.fappel.swt.SWTEventHelper.trigger;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.fappel.swt.DisplayHelper;
import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.animation.AniEvent.Type;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;

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
		Realm.runWithDefault(SWTObservables.getRealm(shell.getDisplay()), new Runnable() {

			@Override
			public void run() {
				uut.createContents(shell);	
			}
			
		});
		
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		
		uut.animationHandler = new  AnimationHandler(null,uut.clock,dmd);
		uut.animationHandler.setScale(uut.scale);
		uut.animationHandler.setEventHandler(eventHandler);
		uut.paletteHandler = new PaletteHandler(uut, shell);

		uut.onNewProject();
		
		uut.createBindings();
		
		byte[] digest = {1,0,0,0};
		uut.hashes.add(digest);
		byte[] emptyFrameDigest = { (byte)0xBF, 0x61, (byte)0x9E, (byte)0xAC, 0x0C, (byte)0xDF, 0x3F, 0x68,
				(byte)0xD4, (byte)0x96, (byte)0xEA, (byte)0x93, 0x44, 0x13, 0x7E, (byte)0x8B };
		uut.hashes.add(emptyFrameDigest);
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
		uut.aniAction = new AnimationActionHandler(uut, shell);
		
		uut.aniAction.loadAni("./src/test/resources/drwho-dump.txt.gz", false, true);
		
		SelectionChangedEvent e = new SelectionChangedEvent(uut.keyframeTableViewer, 
				new StructuredSelection(palMapping));
		fireSelectionChanged(uut.keyframeTableViewer, e);
	}
	
	@Test
	public void testNotifyAniClear() throws Exception {
		//Animation actAnimation = new CompiledAnimation(AnimationType.COMPILED,"foo",0,0,0,0,0);
		AniEvent evt = new AniEvent(Type.CLEAR);
		uut.notifyAni(evt );
	}

	@Test
	public void testNotifyAniAni() throws Exception {
		Animation actAnimation = new CompiledAnimation(AnimationType.COMPILED,"foo",0,0,0,0,0);
		AniEvent evt = new AniEvent(Type.ANI, actAnimation, null);
		uut.notifyAni(evt);
	}

	@Test
	public void testNotifyAniClock() throws Exception {
		//Animation actAnimation = new CompiledAnimation(AnimationType.COMPILED,"foo",0,0,0,0,0);
		AniEvent evt = new AniEvent(Type.CLOCK);
		uut.notifyAni(evt );
	}
	
	@Test
	public void testLoadProjectString() throws Exception {
		uut.shell = shell;
		uut.aniAction = new AnimationActionHandler(uut,shell);
		uut.loadProject("./src/test/resources/test.xml");
		assertThat(uut.recordings.size(), equalTo(2));
		uut.loadProject("./src/test/resources/test.xml");
		assertThat(uut.recordings.size(), equalTo(2));
	}
	
	@Test
	@Ignore
	public void testOpen() throws Exception {
		uut.shell = shell;
		uut.loadProject("./src/test/resources/test.xml");
		displayHelper.getDisplay().timerExec(1000,()->{
			trigger(SWT.Close).on(shell);
		});
		//uut.open(new String[]{});
	}

	
	@Test
	public void testPaletteTypeChanged() throws Exception {
		ISelection s = new StructuredSelection(PaletteType.DEFAULT);
		SelectionChangedEvent e = new SelectionChangedEvent(uut.paletteTypeComboViewer, s );
		uut.onPaletteTypeChanged(e);
	}
	
	@Test
	public void testRemoveAni() throws Exception {
		Animation animation = new Animation(AnimationType.PNG, "test", 0, 0, 0, 0, 0);
		animation.setDesc("foo");
		animation.setMutable(false);
		uut.recordings.put("foo", animation );
		uut.selectedRecording.set(animation);
		trigger(SWT.Selection).on(uut.btnRemoveAni);
		
		assertThat( uut.recordings.isEmpty(), equalTo(true));
	}
	
	@Test
	public void testDeleteKeyframe() throws Exception {
		trigger(SWT.Selection).on(uut.btnDeleteKeyframe);
		PalMapping palMapping = new PalMapping(0,"foo");
		palMapping.animationName = "drwho-dump";
		palMapping.frameIndex = 0;

		uut.selectedPalMapping = palMapping; 
		trigger(SWT.Selection).on(uut.btnDeleteKeyframe);
	}
	
	@Test
	public void testFetchDuration() throws Exception {
		PalMapping palMapping = new PalMapping(0,"foo");
		palMapping.animationName = "drwho-dump";
		palMapping.frameIndex = 0;

		uut.selectedPalMapping = palMapping; 
		trigger(SWT.Selection).on(uut.btnFetchDuration);
	}

	
	@Test
	public void testCreateNewPalette() {
		assertThat(uut.activePalette, notNullValue());
		
		trigger(SWT.Selection).on(uut.btnNewPalette);
		assertThat(uut.activePalette, notNullValue());
		assertThat(uut.project.palettes.size(), equalTo(10));
		
		// test that new palette is selected
		Palette palette = uut.project.palettes.get(9);
		Object element = ((StructuredSelection)uut.paletteComboViewer.getSelection()).getFirstElement();
		assertThat(palette,equalTo(element));
	}
	@Test
	public void testOnlyDefaultPalette() {
		assertThat(uut.activePalette, notNullValue());
		assertThat(uut.project.palettes.size(), equalTo(9));
	}
	
	@Test
	public void testRenamePalette() {
		assertThat(uut.activePalette, notNullValue());
		assertThat(uut.activePalette.name, equalTo("pal0"));
		
		uut.paletteComboViewer.getCombo().setText("2 - foo");
		trigger(SWT.Selection).on(uut.btnRenamePalette);
		assertThat(uut.activePalette, notNullValue());
		assertThat(uut.activePalette.name, equalTo("foo"));
	}
	
	@Test
	public void testAniStop() {
		uut.animationHandler.stop();
		List<Animation> anis = new ArrayList<>();
		CompiledAnimation animation = new CompiledAnimation(AnimationType.COMPILED, "", 0, 0, 0, 0, 0);
		animation.frames.add( new Frame());
		anis.add(animation);
		uut.animationHandler.setAnimations(anis);
		assertThat(uut.drawToolBar.getEnabled(), equalTo(true));
	}

	@Test
	public void testAniStart() {
		uut.animationHandler.start();
		assertThat(uut.drawToolBar.getEnabled(), equalTo(false));
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
		animation.setMutable(true);
		uut.recordings.put("foo", animation );

		// frameSeqView must have a selection
		uut.buildFrameSeqList();
		uut.frameSeqViewer.setSelection(new StructuredSelection(uut.frameSeqList.get(0)), true);
		byte[] digest = {1,0,0,0};
		trigger(SWT.Selection).on(uut.btnAddFrameSeq);
		assertThat(uut.project.palMappings.size(), equalTo(1));
		PalMapping mapping = uut.project.palMappings.get(0);
		assertThat(mapping.name, equalTo("KeyFrame foo"));
		assertThat(mapping.crc32, equalTo(digest));
		assertThat(mapping.frameSeqName, equalTo("foo"));
	}

	@Test
	public void testMaskNumberChanged() throws Exception {
		Event e = new Event();
		e.widget = Mockito.mock(Spinner.class);
		uut.onMaskNumberChanged(e);
	}

	@Test
	public void testMaskNumberChangedUse() throws Exception {
		Event e = new Event();
		Spinner s = Mockito.mock(Spinner.class);
		e.widget = s;
		uut.useGlobalMask = true;
		when(s.getSelection()).thenReturn(Integer.valueOf(1));
		uut.onMaskNumberChanged(e);
	}

}
