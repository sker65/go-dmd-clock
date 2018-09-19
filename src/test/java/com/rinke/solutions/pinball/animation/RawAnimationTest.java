package com.rinke.solutions.pinball.animation;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;

public class RawAnimationTest {
	
	RawAnimation uut;
	private DMD dmd = new DMD(128,32);

	@Before
	public void setUp() throws Exception {
		uut = new RawAnimation(AnimationType.RAW, "foo", 0, 0, 1, 0, 0);
	}

	@Test
	public void testAddFrameFrame() throws Exception {
		uut.addFrame(new Frame());
	}

	@Test
	public void testRenderSubframes() throws Exception {
		uut.renderSubframes(dmd , 0);
	}

}
