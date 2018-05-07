package com.rinke.solutions.pinball.animation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnimationFactoryTest {
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCreateAnimationsFromProperties() throws Exception {
		AnimationFactory.createAnimationsFromProperties("./src/test/resources/renderer/animation.properties", null);
	}

}
