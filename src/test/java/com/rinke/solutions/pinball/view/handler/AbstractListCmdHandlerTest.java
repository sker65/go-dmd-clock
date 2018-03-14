package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class AbstractListCmdHandlerTest {
	
	@Mock
	private AnimationHandler animationHandler;
	
	private AbstractListCmdHandler uut;
	
	private ViewModel vm;
	
	@Before
	public void setup() {
		this.vm = new ViewModel();
		uut = new AbstractListCmdHandler(vm);
		uut.setAnimationHandler(animationHandler);
	}

	@Test
	public void testUpdateAnimationMapKey() throws Exception {
		Animation animation = new Animation(AnimationType.COMPILED, "foo", 0, 1, 0, 1, 1);
		animation.setDesc("new");
		vm.recordings.put("old", animation);
		uut.updateAnimationMapKey("old", "new", vm.recordings);
		assertTrue(vm.recordings.get("new") != null);
	}

	@Test
	public void testOnRemove() throws Exception {
		//throw new RuntimeException("not yet implemented");
	}

	@Test
	public void testOnSortAnimations() throws Exception {
		//throw new RuntimeException("not yet implemented");
	}

}
