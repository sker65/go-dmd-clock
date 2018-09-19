package com.rinke.solutions.pinball.renderer;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.DMD;

public class VPinMameRawRendererTest {
	
	VPinMameRawRenderer uut;
	private DMD dmd = new DMD(128,32);

	@Before
	public void setUp() throws Exception {
		uut = new VPinMameRawRenderer();
	}

	@Test
	public void testReadImage() throws Exception {
		uut.readImage("./src/test/resources/renderer/tz_92.raw.gz", dmd );
	}

}
