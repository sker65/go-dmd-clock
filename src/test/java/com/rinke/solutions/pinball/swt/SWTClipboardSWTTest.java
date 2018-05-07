package com.rinke.solutions.pinball.swt;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.eclipse.swt.dnd.Clipboard;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SWTClipboardSWTTest {
	
	@Mock
	private Clipboard clipboard;
	
	@InjectMocks
	private SWTClipboard uut;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetContents() throws Exception {
		uut.getContents("foo");
	}

	@Test
	public void testSetContents() throws Exception {
		uut.setContents(new Object[]{"String"}, new String[]{"ImageTransfer"});
	}

	@Test
	public void testGetAvailableTypeNames() throws Exception {
		when( clipboard.getAvailableTypeNames()).thenReturn(new String[]{"foo"});
		String[] availableTypeNames = uut.getAvailableTypeNames();
		assertEquals("foo", availableTypeNames[0]);
	}

}
