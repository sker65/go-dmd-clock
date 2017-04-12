package com.rinke.solutions.pinball.renderer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;

public class VPinMameRendererTest {
	
	VPinMameRenderer uut;
	
	@Before
	public void setup() {
		uut = new VPinMameRenderer();
	}

	@Test
	public void testReadImage() throws Exception {
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		uut.readImage("./src/test/resources/drwho-dump.txt.gz", dmd );
		assertEquals(10594, uut.frames.size());
		assertEquals(2, uut.getNumberOfPlanes());
	}

	@Test
	public void testReadImage2() throws Exception {
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		uut.readImage("./src/test/resources/renderer/avg_170_dump.txt.gz", dmd );
		assertEquals(2119, uut.frames.size());
		assertEquals(4, uut.getNumberOfPlanes());
	}
}
