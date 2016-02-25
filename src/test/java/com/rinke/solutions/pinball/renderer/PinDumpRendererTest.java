package com.rinke.solutions.pinball.renderer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.rinke.solutions.pinball.DMD;

public class PinDumpRendererTest {
	
	String base = "./src/test/resources/renderer/";
	
	PinDumpRenderer uut = new PinDumpRenderer();

	@Test
	public void testReadImage() throws Exception {
		DMD dmd = new DMD(128,32);
		uut.readImage(base+"real_tz.dump.gz", dmd );
		assertEquals(632, uut.getFrames().size());
	}

}
