package com.rinke.solutions.pinball;

import static org.junit.Assert.*;

import java.util.Observable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MaskDmdObserverTest {
	
	@Mock
	DMD mask;
	@Mock
	DMD dmd;
	@Mock
	Observable obs;
	
	@InjectMocks
	MaskDmdObserver uut;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCanUndo() throws Exception {
		assertFalse(uut.canUndo());
	}

	@Test
	public void testCanRedo() throws Exception {
		uut.canRedo();
	}

	@Test
	public void testRedo() throws Exception {
		uut.redo();
	}

	@Test
	public void testUndo() throws Exception {
		uut.undo();
	}

	@Test
	public void testUpdate() throws Exception {
		DMD mask = new DMD(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
		uut.setMask(mask);
		mask.notifyObservers();
		assertFalse(uut.hasChanged());
		// subscribe to this observer and see if it gets called
//		verify(mask).notifyObservers();
	}

}
