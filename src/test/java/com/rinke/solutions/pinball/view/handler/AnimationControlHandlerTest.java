package com.rinke.solutions.pinball.view.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;

@RunWith(MockitoJUnitRunner.class)
public class AnimationControlHandlerTest extends HandlerTest {
	
	@Mock
	private AnimationHandler animationHandler;
	
	private AnimationControlHandler uut;

	@Before
	public void setUp() throws Exception {
		uut = new AnimationControlHandler(vm);
		uut.animationHandler = animationHandler;
	}

	@Test
	public void testOnUpdateDelay() throws Exception {
		uut.onUpdateDelay();
		vm.setSelectedScene(ani);
		uut.onUpdateDelay();
	}

}
