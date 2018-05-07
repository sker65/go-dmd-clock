package com.rinke.solutions.pinball.renderer;

import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.PinDmdEditor;


public class PcapRendererTest extends RendererTest {
	
	PcapRenderer uut = new PcapRenderer();

	@Test
	public void testReadImage() throws Exception {
		uut.readImage("./src/test/resources/renderer/tz.pcap.gz", dmd );
	}

}
