package com.rinke.solutions.pinball.renderer;

import org.junit.Ignore;
import org.junit.Test;

import com.rinke.solutions.pinball.DMD;

public class VideoCapRendererTest {
	
	VideoCapRenderer uut = new VideoCapRenderer(0,0);

	@Test
	
	@Ignore
	public void testConvert() throws Exception {
		DMD dmd = new DMD(128,32);
		String name = "/Users/stefanri/Downloads/roadrunner.mp4";
		uut.convert(name , dmd , 0);
	}

}
