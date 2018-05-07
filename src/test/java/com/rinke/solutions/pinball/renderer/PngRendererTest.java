package com.rinke.solutions.pinball.renderer;

import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;

public class PngRendererTest {
	
	String base = "./src/test/resources/renderer/";
	
	PngRenderer uut = new PngRenderer();


	@Test
	public void testConvert() throws Exception {
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		uut.convert(base, dmd, 0x06A0);
	}

}
