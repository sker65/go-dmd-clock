package com.rinke.solutions.pinball.renderer;

import org.junit.Test;

public class PngRendererTest extends RendererTest {
	
	String base = "./src/test/resources/renderer/";
	
	PngRenderer uut = new PngRenderer();

	@Test
	public void testConvert() throws Exception {
		uut.convert(base, dmd, 0x06A0);
	}

}
