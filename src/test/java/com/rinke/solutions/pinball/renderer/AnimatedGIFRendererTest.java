package com.rinke.solutions.pinball.renderer;

import org.junit.Ignore;
import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;

public class AnimatedGIFRendererTest {
	
	String base = "./src/test/resources/renderer/";
	
	AnimatedGIFRenderer renderer = new AnimatedGIFRenderer();

	@Test
	public void testReadImage() throws Exception {
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		renderer.convert(base + "ezgif-645182047.gif", dmd, 0);
	}

	@Test
	public void testReadPalette() throws Exception {
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		renderer.convert(base+"lotr_4bit.gif", dmd, 0);
	}

}
