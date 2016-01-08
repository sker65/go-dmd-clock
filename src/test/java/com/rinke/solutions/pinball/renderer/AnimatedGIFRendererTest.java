package com.rinke.solutions.pinball.renderer;

import org.junit.Test;

import com.rinke.solutions.pinball.DMD;

public class AnimatedGIFRendererTest {
	
	String base = "./src/test/resources/renderer/";
	
	AnimatedGIFRenderer renderer = new AnimatedGIFRenderer();

	@Test
	public void testReadImage() throws Exception {
		DMD dmd = new DMD(128, 32);
		renderer.convert(base + "ezgif-645182047.gif", dmd, 0);
	}

}
