package com.rinke.solutions.pinball.renderer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class VPinMameRendererTest extends RendererTest {
	
	VPinMameRenderer uut;
	
	@Before
	public void setup() {
		uut = new VPinMameRenderer();
	}

	@Test
	public void testReadImage() throws Exception {
		uut.readImage("./src/test/resources/drwho-dump.txt.gz", dmd );
		assertEquals(10594, uut.frames.size());
		assertEquals(2, uut.getNumberOfPlanes());
	}

	@Test
	public void testReadImage2() throws Exception {
		uut.readImage("./src/test/resources/renderer/avg_170_dump.txt.gz", dmd );
		assertEquals(2119, uut.frames.size());
		assertEquals(4, uut.getNumberOfPlanes());
	}

	@Test
	public void testHex2int() throws Exception {
		assertEquals(15, uut.hex2int('f'));
		assertEquals(15, uut.hex2int('F'));
		assertEquals(9, uut.hex2int('9'));
	}
}
