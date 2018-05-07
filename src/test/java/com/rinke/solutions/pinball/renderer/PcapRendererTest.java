package com.rinke.solutions.pinball.renderer;

import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;


public class PcapRendererTest {
	
	PcapRenderer uut = new PcapRenderer();

	@Test
	public void testReadImage() throws Exception {
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		uut.readImage("./src/test/resources/renderer/tz.pcap.gz", dmd );
	}

}
