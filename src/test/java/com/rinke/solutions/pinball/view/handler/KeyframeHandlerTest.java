package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.view.model.ViewModel;


public class KeyframeHandlerTest {
	
	KeyframeHandler uut;
	private ViewModel vm;
	
	@Before
	public void setup() {
		vm = new ViewModel();
		uut = new KeyframeHandler(vm);
	}

	@Test
	public void testCheckForDuplicateKeyFrames() throws Exception {
		PalMapping p = new PalMapping(0, "foo");
		p.crc32 = new byte[] { 1, 2, 3, 4 };
		assertFalse(uut.checkForDuplicateKeyFrames(p));
		vm.keyframes.put(p.name,p);
		assertTrue(uut.checkForDuplicateKeyFrames(p));
	}


}
