package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.view.model.ViewModel;

public class MaskHandlerTest {
	
	MaskHandler uut;
	ViewModel vm;
	
	@Before
	public void setup() {
		vm = new ViewModel();
		uut = new MaskHandler(vm);
	}

	@Test
	public void testOnInvertMask() throws Exception {
		byte[] data = new byte[512];
		vm.dmd.setMask(data);
		uut.onInvertMask();
		assertEquals((byte) 0xFF, (byte) vm.dmd.getFrame().mask.data[0]);
	}


}
