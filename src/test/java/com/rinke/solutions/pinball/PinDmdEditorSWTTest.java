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
import org.mockito.Mock;
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
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.widget.DMDWidget;

import static com.fappel.swt.JFaceViewerHelper.fireSelectionChanged;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class PinDmdEditorSWTTest {
	
	//@InjectMocks
	private PinDmdEditor uut = new PinDmdEditor();
	
	@Mock
	DMDWidget dmdWidget;
	
	@Mock
	DMDWidget previewDMD;
	
	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	Shell shell;
	
	public EventHandler eventHandler = new EventHandler() {
		
		@Override
		public void notifyAni(AniEvent evt) {
			
		}
	};

	private ViewModel vm;
	
/*	@Before
	public void setup() {
		shell = displayHelper.createShell();
		uut.v.shell = shell;
		Realm.runWithDefault(SWTObservables.getRealm(shell.getDisplay()), new Runnable() {

			@Override
			public void run() {
				uut.v.createContents();	
			}
			
		});
		
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		
		uut.animationHandler = new  AnimationHandler(null,uut.clock,dmd);
		//uut.animationHandler.setScale(uut.v.frame);
		uut.animationHandler.setEventHandler(eventHandler);
		vm = new ViewModel();
		uut.paletteHandler = new PaletteHandler(vm);

		uut.onNewProject();
		
		uut.createBindings();
		
		byte[] digest = {1,0,0,0};
		vm.hashes.add(digest);
		byte[] emptyFrameDigest = { (byte)0xBF, 0x61, (byte)0x9E, (byte)0xAC, 0x0C, (byte)0xDF, 0x3F, 0x68,
				(byte)0xD4, (byte)0x96, (byte)0xEA, (byte)0x93, 0x44, 0x13, 0x7E, (byte)0x8B };
		vm.hashes.add(emptyFrameDigest);
		uut.v.dmdWidget = dmdWidget;
		uut.v.previewDmd = dmdWidget;
	}*/
	
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
	
	
/*	@Test
	
	@Test
	public void testNotifyAniClear() throws Exception {
		//Animation actAnimation = new CompiledAnimation(AnimationType.COMPILED,"foo",0,0,0,0,0);
		AniEvent evt = new AniEvent(Type.CLEAR);
		uut.notifyAni(evt );
	}

	@Test
	public void testNotifyAniAni() throws Exception {
		Animation actAnimation = new CompiledAnimation(AnimationType.COMPILED,"foo",0,0,0,0,0);
		AniEvent evt = new AniEvent(Type.ANI, actAnimation, new Frame());
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
		uut.v.shell = shell;
		uut.aniAction = new AnimationActionHandler(uut);
		uut.loadProject("./src/test/resources/test.xml");
		assertThat(vm.recordings.size(), equalTo(1));
		assertThat(vm.scenes.size(), equalTo(1));
		uut.loadProject("./src/test/resources/test.xml");
		assertThat(vm.recordings.size(), equalTo(1));
		assertThat(vm.scenes.size(), equalTo(1));
	}
	
	@Test
	public void testOpen() throws Exception {
		Object monitor = new Object();
		uut.v.shell = shell;
		uut.aniAction = new AnimationActionHandler(uut);
		uut.loadProject("./src/test/resources/test.xml");
		displayHelper.getDisplay().timerExec(500,()->{
			trigger(SWT.Close).on(shell);
			synchronized (monitor) { monitor.notify(); }
		});
		synchronized (monitor) {
			monitor.wait(5000);
		}
	}

	
//	@Test
//	public void testPaletteTypeChanged() throws Exception {
//		ISelection s = new StructuredSelection(PaletteType.DEFAULT);
//		SelectionChangedEvent e = new SelectionChangedEvent(uut.v.paletteTypeComboViewer, s );
//		uut.onPaletteTypeChanged(e);
//	}
	
	@Test
	public void testRemoveAni() throws Exception {
		Animation animation = new Animation(AnimationType.PNG, "test", 0, 0, 0, 0, 0);
		animation.setDesc("foo");
		animation.setMutable(false);
		vm.recordings.put("foo", animation );
		vm.setSelectedRecording(animation);
		trigger(SWT.Selection).on(uut.v.deleteRecording);
		assertThat( vm.recordings.isEmpty(), equalTo(true));
	}
	
	@Test
	public void testDeleteKeyframe() throws Exception {
		trigger(SWT.Selection).on(uut.v.deleteKeyFrame);
		PalMapping palMapping = new PalMapping(0,"foo");
		palMapping.animationName = "drwho-dump";
		palMapping.frameIndex = 0;

		vm.setSelectedKeyFrame(palMapping); 
		trigger(SWT.Selection).on(uut.v.deleteKeyFrame);
	}
	
	//test in handler directly
	@Test
	public void testFetchDuration() throws Exception {
		PalMapping palMapping = new PalMapping(0,"foo");
		palMapping.animationName = "drwho-dump";
		palMapping.frameIndex = 0;

		vm.setSelectedKeyFrame(palMapping); 
		trigger(SWT.Selection).on(uut.v.btnFetchDuration);
	}*/

	
/*	@Test
	public void testCreateNewPalette() {
		assertThat(vm.selectedPalette, notNullValue());
		
		trigger(SWT.Selection).on(uut.v.btnNewPalette);
		assertThat(vm.selectedPalette, notNullValue());
		assertThat(vm.paletteMap.size(), equalTo(10));
		
		// test that new palette is selected
		Palette palette = vm.paletteMap.get(9);
		Object element = ((StructuredSelection)uut.v.paletteComboViewer.getSelection()).getFirstElement();
		assertThat(palette,equalTo(element));
	}*/
	
	@Test
	public void testOnlyDefaultPalette() {
		assertThat(vm.selectedPalette, notNullValue());
		assertThat(vm.paletteMap.size(), equalTo(9));
	}
	
	
	@Test
	public void testAniStop() {
		uut.animationHandler.stop();
		List<Animation> anis = new ArrayList<>();
		CompiledAnimation animation = new CompiledAnimation(AnimationType.COMPILED, "", 0, 0, 0, 0, 0);
		animation.frames.add( new Frame());
		anis.add(animation);
		uut.animationHandler.setAnimations(anis);
		// TEST HANDLER assertThat(uut.v.drawToolBar.getEnabled(), equalTo(true));
	}

	@Test
	public void testAniStart() {
		uut.animationHandler.start();
		//assertThat(uut.v.drawToolBar.getEnabled(), equalTo(false));
	}
	
	@Test
	public void testAddPalSwitch() {
		Animation animation = new Animation(AnimationType.PNG, "test", 0, 0, 0, 0, 0);
		animation.setDesc("foo");
		animation.setMutable(false);
		vm.recordings.put("foo", animation );
		vm.setSelectedRecording(animation);
		
		/*trigger(SWT.Selection).on(uut.v.btnAddKeyframe);
		assertThat(uut.project.palMappings.size(), equalTo(1));
		assertThat(uut.project.palMappings.get(0).name, equalTo("KeyFrame 1"));*/
	}

	@Test
	public void testAddFrameSeq() {
		
		CompiledAnimation animation = new CompiledAnimation(AnimationType.PNG, "test", 0, 0, 0, 0, 0);
		animation.setDesc("foo");
		animation.setMutable(true);
		vm.scenes.put("foo", animation );
		
		CompiledAnimation recording = new CompiledAnimation(AnimationType.PNG, "test", 0, 0, 0, 0, 0);
		animation.setDesc("foo2");
		vm.recordings.put("foo2", recording );
		vm.setSelectedRecording(recording);

		// frameSeqView must have a selection
		// vm.populateFrameSeqList();
		// uut.v.frameSeqViewer.setSelection(new StructuredSelection(uut.frameSeqList.get(0)), true);
		vm.setSelectedFrameSeq(recording);
		
/*		byte[] digest = {1,0,0,0};
		trigger(SWT.Selection).on(uut.v.btnAddFrameSeq);
		assertThat(uut.project.palMappings.size(), equalTo(1));
		PalMapping mapping = uut.project.palMappings.get(0);
		assertThat(mapping.name, equalTo("KeyFrame foo2"));
		assertThat(mapping.crc32, equalTo(digest));
		assertThat(mapping.frameSeqName, equalTo("foo2"));*/
	}

	@Test
	public void testMaskNumberChanged() throws Exception {
		Event e = new Event();
		e.widget = Mockito.mock(Spinner.class);
		// without binding trigger the change method directly
		vm.setSelectedMask(0);
	}

	@Test
	public void testMaskNumberChangedUse() throws Exception {
		Event e = new Event();
		Spinner s = Mockito.mock(Spinner.class);
		e.widget = s;
		vm.setUseGlobalMask( true );
		when(s.getSelection()).thenReturn(Integer.valueOf(1));
		// without binding trigger the change method directly
		vm.setSelectedMask(1);
	}

}
