package com.rinke.solutions.pinball.animation;

import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AnimationFactoryTest {
	
	@Mock
	Shell shell;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCreateAnimationsFromProperties() throws Exception {
		AnimationFactory.createAnimationsFromProperties("./src/test/resources/renderer/animation.properties", shell);
	}

}
