package com.rinke.solutions.pinball.view.model;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.util.Config;

public class ViewModelTest {

	private ViewModel uut;
	DmdSize size = DmdSize.Size128x32;
	private DMD dmd = new DMD(size);

	@Before
	public void setUp() throws Exception {
		uut = new ViewModel();
	}

	@Test
	public void testResetMask() throws Exception {
		uut.resetMask(DmdSize.Size128x32, 10);
	}

	@Test
	public void testInit() throws Exception {
		uut.init(dmd, size, size, "foo", 10, new Config());
	}

}
