package com.rinke.solutions.pinball.renderer;

import static org.junit.Assert.*;

import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.model.Frame;

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

	@Test
	public void testReadImage192() throws Exception {
		dmd = new DMD(DmdSize.Size192x64);
		Frame f = renderer.convert(base+"b1.gif", dmd, 0);
		assertEquals( 1536, f.planes.get(0).data.length);
	}
}
