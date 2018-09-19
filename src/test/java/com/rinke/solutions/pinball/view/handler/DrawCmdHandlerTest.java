package com.rinke.solutions.pinball.view.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.MaskDmdObserver;
import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.AniEvent.Type;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.widget.DMDWidget.Rect;

@RunWith(MockitoJUnitRunner.class)
public class DrawCmdHandlerTest extends HandlerTest {
	@Mock
	private AnimationHandler animationHandler;

	@Mock
	private HashCmdHandler hashCmdHandler;

	@Mock
	private LivePreviewHandler livePreviewHandler;

	@Mock
	private MaskDmdObserver maskDmdObserver;

	@Mock
	private MessageUtil messageUtil;

	@Mock
	private RecordingsCmdHandler recordingsCmdHandler;
	
	@InjectMocks
	private DrawCmdHandler uut = new DrawCmdHandler(vm, vm.dmd);

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testOnSelectedEditModeChanged() throws Exception {
		vm.setSelectedScene(ani);
		uut.onSuggestedEditModeChanged(EditMode.REPLACE, EditMode.COLMASK);
		uut.onSuggestedEditModeChanged(EditMode.COLMASK, EditMode.REPLACE);
	}

	@Test
	public void testOnSelectedEditModeChangedWithLocalMask() throws Exception {
		vm.setSelectedScene(ani);
		uut.onSuggestedEditModeChanged(EditMode.REPLACE, EditMode.COLMASK_FOLLOW);
	}

	@Test
	public void testOnSelectedEditModeChangedWithVeto() throws Exception {
		ani.setDirty(true);
		vm.setSelectedScene(ani);
		uut.onSuggestedEditModeChanged(EditMode.REPLACE, EditMode.COLMASK);
	}

	@Test
	public void testOnDrawingEnabledChanged() throws Exception {
		uut.onDrawingEnabledChanged(false, true);
	}

	@Test
	public void testOnSelectedFrameChanged() throws Exception {
		uut.onSelectedFrameChanged(0, 1);
	}

	@Test
	public void testOnSelectionChanged() throws Exception {
		Rect r = new Rect(0, 0, 10, 20);
		uut.onSelectionChanged(null, r );
		uut.onSelectionChanged(r, null );
	}

	@Test
	public void testNotifyAni() throws Exception {
		AniEvent evt = new AniEvent(Type.CLEAR);
		uut.notifyAni(evt);
	}

	@Test
	public void testNotifyAniTypeFrameChange() throws Exception {
		AniEvent evt = new AniEvent(Type.FRAMECHANGE, getScene("foo"), new Frame() );
		uut.notifyAni(evt );
	}

	@Test
	public void testNotifyAniClockType() throws Exception {
		AniEvent evt = new AniEvent(Type.CLOCK, getScene("foo"), new Frame() );
		uut.notifyAni(evt );
	}

	@Test
	public void testNotifyAniAniType() throws Exception {
		AniEvent evt = new AniEvent(Type.ANI, getScene("foo"), new Frame() );
		uut.notifyAni(evt );
	}

	@Test
	public void testOnAddFrame() throws Exception {
		vm.selectedScene = getScene("foo");
		uut.onAddFrame();
	}

	@Test
	public void testOnRemoveFrame() throws Exception {
		vm.selectedScene = getScene("foo");
		uut.onRemoveFrame();
	}

}
