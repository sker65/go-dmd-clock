package com.rinke.solutions.pinball.widget;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;

public class FloodFillToolTest {
	
	FloodFillTool uut = new FloodFillTool(1);

	@Before
	public void setUp() throws Exception {
		uut.setDMD(new DMD(DmdSize.Size128x32));
	}

	@Test
	public void testMouseUp() throws Exception {
		uut.mouseUp(1, 1);
	}

}
