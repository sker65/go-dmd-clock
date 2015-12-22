package com.rinke.solutions.pinball.renderer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.DMD;

public class VPinMameRendererTest {
	
	VPinMameRenderer uut;
	
	@Before
	public void setup() {
		uut = new VPinMameRenderer();
	}

	@Test
	public void testReadImage() throws Exception {
		DMD dmd = new DMD(128,32);
		uut.readImage("./src/test/resources/drwho-dump.txt.gz", dmd );
		assertEquals(10594, uut.frames.size());
	}

}
