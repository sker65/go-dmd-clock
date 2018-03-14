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
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class CutCmdHandlerTest extends HandlerTest {
	@Mock
	private BookmarkHandler bookmarkHandler;

	@Mock
	private PaletteHandler paletteHandler;
	
	private CutCmdHandler uut;
	
	@Before
	public void setup() {
		uut = new CutCmdHandler(vm);
		uut.paletteHandler = paletteHandler;
	}

	@Test
	public void testBuildUniqueName() throws Exception {
		Animation animation = getAnimation();
		vm.recordings.put("Scene 1", animation);
		String actual = uut.buildUniqueName(vm.recordings);
		assertNotEquals("Scene 1", actual);
	}

	CompiledAnimation getAnimation() {
		CompiledAnimation animation = new CompiledAnimation(AnimationType.COMPILED, "foo", 0, 1, 0, 1, 1);
		animation.setDesc("new");
		return animation;
	}

	@Test
	public void testCutScene() throws Exception {
		CompiledAnimation animation = getAnimation();
		byte[] plane1 = new byte[vm.dmdSize.planeSize];
		animation.frames.add(new Frame(plane1, plane1));
		uut.cutScene(animation, 0, 0, "1");
	}


}
