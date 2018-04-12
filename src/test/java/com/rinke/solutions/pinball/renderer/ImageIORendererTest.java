package com.rinke.solutions.pinball.renderer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.DMD;

@RunWith(MockitoJUnitRunner.class)
public class ImageIORendererTest {
	
	
	@InjectMocks
	private ImageIORenderer uut = new ImageIORenderer("%d.png");

	private DMD dmd = new DMD(128,32);

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testConvertStringDMDInt() throws Exception {
		uut.convert("./src/test/resources", dmd , 1);
	}

}
