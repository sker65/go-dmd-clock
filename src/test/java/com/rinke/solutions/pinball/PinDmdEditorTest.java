package com.rinke.solutions.pinball;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.Frame;
import com.rinke.solutions.pinball.animation.Plane;

@RunWith(MockitoJUnitRunner.class)
public class PinDmdEditorTest {
	
	//@InjectMocks
	private PinDmdEditor pinDmdEditor = new PinDmdEditor();

	@Test
	public void testCutScene() throws Exception {
		Animation src = new Animation(AnimationType.MAME,
				"./src/test/resources/drwho-dump.txt.gz", 0, 100, 0, 0, 0);
		Animation cutScene = pinDmdEditor.cutScene(src, 10, 20);
		assertThat(cutScene, notNullValue());
		assertThat(cutScene.end - cutScene.start, equalTo(10));

		src.actFrame = 10;
		Frame srcFrame = src.render(new DMD(128, 32), false);
		cutScene.actFrame = 0;
		Frame destFrame = cutScene.render(new DMD(128, 32), false);
		assertThat(srcFrame.planes.size(), equalTo(destFrame.planes.size()));
		int i = 0;
		for (Plane p : srcFrame.planes) {
			assertThat(p.plane, equalTo(destFrame.planes.get(i++).plane));
		}

	}

}
