package com.rinke.solutions.pinball.view.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;

@RunWith(MockitoJUnitRunner.class)
public class AnimationControlHandlerTest extends HandlerTest {
	
	@Mock
	private AnimationHandler animationHandler;
	@Mock
	private MaskHandler maskHandler;
	
	@InjectMocks
	private AnimationControlHandler uut = new AnimationControlHandler(vm);

	@Before
	public void setUp() throws Exception {
		vm.selectedEditMode = EditMode.REPLACE;
		vm.setSelectedScene(getScene("foo"));
		vm.hashes.add(getHash());
	}

	@Test
	public void testOnUpdateDelay() throws Exception {
		uut.onUpdateDelay();
		vm.setSelectedScene(ani);
		uut.onUpdateDelay();
	}

	@Test
	public void testOnNextFrame() throws Exception {
		uut.onNextFrame();
	}

	@Test
	public void testOnNextFrameNoHash() throws Exception {
		vm.setSelectedHashIndex(-1);
		uut.onNextFrame();
	}

	@Test
	public void testOnStartStop() throws Exception {
		uut.onStartStop(true);
	}

	@Test
	public void testOnStartStopStopping() throws Exception {
		uut.onStartStop(false);
	}

	@Test
	public void testOnPrevFrame() throws Exception {
		uut.onPrevFrame();
	}

	@Test
	public void testOnCopyAndMoveToNextFrame() throws Exception {
		CompiledAnimation ani = vm.selectedScene;
		ani.actFrame = 1;
		uut.onCopyAndMoveToNextFrame();
	}

	@Test
	public void testOnCopyAndMoveToPrevFrame() throws Exception {
		uut.onCopyAndMoveToPrevFrame();
	}

}
