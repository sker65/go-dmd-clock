package com.rinke.solutions.pinball;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.view.handler.HandlerTest;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class AnimationHandlerTest extends HandlerTest{
		
	@Mock EventHandler eventHandler;
	
	@InjectMocks
	private AnimationHandler uut = new AnimationHandler(vm, new DMDClock(), new DMD(128,32));

	private CompiledAnimation scene;

	@Before
	public void setUp() throws Exception {
		vm.setSelectedEditMode(EditMode.COLMASK);
		List<Animation> anis = new ArrayList<>();
		scene = getScene("foo");
		anis.add(scene);
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

	@Test
	public void testRunInner() throws Exception {
		uut.setEventHandler(new EventHandler() {	
			@Override
			public void notifyAni(AniEvent evt) {
				assertEquals(10,evt.frame.delay);
				assertEquals(20,evt.frame.timecode);
			}
		});
		Frame f = new Frame();
		f.delay = 10;
		f.timecode = 20;
		scene.frames.add(f);
		scene.start = 0;
		scene.end = 2;
		uut.setPos(2);
	}

}
