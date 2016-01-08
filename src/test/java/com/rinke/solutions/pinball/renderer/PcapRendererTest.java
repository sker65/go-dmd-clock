package com.rinke.solutions.pinball.renderer;

import org.junit.Test;

import com.rinke.solutions.pinball.DMD;


public class PcapRendererTest {
	
	PcapRenderer uut = new PcapRenderer();

	@Test
	public void testReadImage() throws Exception {
		DMD dmd = new DMD(128, 32);
		uut.readImage("./src/test/resources/renderer/tz.pcap.gz", dmd );
	}

}
