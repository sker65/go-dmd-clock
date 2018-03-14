package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class CutCmdHandlerTest {
	@Mock
	private BookmarkHandler bookmarkHandler;

	@Mock
	private PaletteHandler paletteHandler;
	
	private CutCmdHandler uut;

	private ViewModel vm;
	
	@Before
	public void setup() {
		this.vm = new ViewModel();
		uut = new CutCmdHandler(vm);
		uut.paletteHandler = paletteHandler;
	}

	@Test
	public void testBuildUniqueName() throws Exception {
		Animation animation = new Animation(AnimationType.COMPILED, "foo", 0, 1, 0, 1, 1);
		animation.setDesc("new");
		vm.recordings.put("Scene 1", animation);
		String actual = uut.buildUniqueName(vm.recordings);
		assertNotEquals("Scene 1", actual);
	}


}
