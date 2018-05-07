package com.rinke.solutions.pinball.swt;

import org.eclipse.swt.dnd.TransferData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.model.Frame;

@RunWith(MockitoJUnitRunner.class)
public class DmdFrameTransferTest {
	
	@InjectMocks
	private DmdFrameTransfer uut;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetTypeIds() throws Exception {
		uut.getTypeIds();
	}

	@Test
	public void testGetInstance() throws Exception {
		DmdFrameTransfer.getInstance();
	}

	@Test
	public void testGetTypeNames() throws Exception {
		uut.getTypeNames();
	}

	@Test
	public void testJavaToNative() throws Exception {
		uut.javaToNative(null, null);
	}

	@Test
	public void testNativeToJava() throws Exception {
		uut.nativeToJava(null);
	}

}
