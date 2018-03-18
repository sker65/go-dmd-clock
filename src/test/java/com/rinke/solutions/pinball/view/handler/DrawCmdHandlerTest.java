package com.rinke.solutions.pinball.view.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.MaskDmdObserver;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.util.MessageUtil;

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
	private DrawCmdHandler drawCmdHandler = new DrawCmdHandler(vm, vm.dmd);

	@Before
	public void setUp() throws Exception {
		
	}

	@Test
	public void testOnSelectedEditModeChanged() throws Exception {
		vm.setSelectedScene(ani);
		drawCmdHandler.onSuggestedEditModeChanged(EditMode.REPLACE, EditMode.COLMASK);
		drawCmdHandler.onSuggestedEditModeChanged(EditMode.COLMASK, EditMode.REPLACE);
	}

}
