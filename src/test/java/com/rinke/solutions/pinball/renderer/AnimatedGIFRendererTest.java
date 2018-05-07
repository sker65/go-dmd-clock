package com.rinke.solutions.pinball.renderer;

import org.junit.Test;

public class AnimatedGIFRendererTest extends RendererTest {
	
	String base = "./src/test/resources/renderer/";
	
	AnimatedGIFRenderer renderer = new AnimatedGIFRenderer();

	@Test
	public void testReadImage() throws Exception {
		renderer.convert(base + "ezgif-645182047.gif", dmd, 0);
	}

	@Test
	public void testReadPalette() throws Exception {
		renderer.convert(base+"lotr_4bit.gif", dmd, 0);
	}

}
