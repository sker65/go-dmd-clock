package com.rinke.solutions.pinball.widget;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;

public class LineToolTest {
	
	LineTool uut = new LineTool(2);

	@Before
	public void setUp() throws Exception {
		uut.setDMD(new DMD(DmdSize.Size128x32));
	}

	@Test
	public void testMouseMove() throws Exception {
		uut.mouseDown(1, 1);
		uut.mouseMove(10, 10);
	}

	@Test
	public void testMouseDown() throws Exception {
		uut.mouseDown(1, 1);
	}


}
