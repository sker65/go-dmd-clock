package com.rinke.solutions.pinball;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.view.handler.HandlerTest;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class AnimationHandlerTest extends HandlerTest{
		
	@Mock EventHandler eventHandler;
	
	@InjectMocks
	private AnimationHandler uut = new AnimationHandler(vm, new DMDClock(), new DMD(128,32));

	@Before
	public void setUp() throws Exception {
		List<Animation> anis = new ArrayList<>();
		anis.add(getScene("foo"));
		uut.setAnimations(anis);
	}

	@Test
	public void testStartClock() throws Exception {
		uut.startClock();
	}

	@Test
	public void testStart() throws Exception {
		uut.start();
	}

	@Test
	public void testStop() throws Exception {
		uut.stop();
	}

	@Test
	public void testPrev() throws Exception {
		uut.prev();
	}

	@Test
	public void testSetPosIntMask() throws Exception {
		uut.setPos(0, null);
	}

	@Test
	public void testSetPosInt() throws Exception {
		uut.setPos(0);
	}

	@Test
	public void testNext() throws Exception {
		uut.next();
	}

	@Test
	public void testGetRefreshDelay() throws Exception {
		uut.getRefreshDelay();
	}

}
