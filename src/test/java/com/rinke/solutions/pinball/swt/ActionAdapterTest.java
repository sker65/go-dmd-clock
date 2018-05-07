package com.rinke.solutions.pinball.swt;

import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ActionAdapterTest {
	@Mock
	private Runnable delegate;
	
	@InjectMocks
	private ActionAdapter uut;

	@Test
	public void testRun() throws Exception {
		uut.run();
		verify(delegate, times(1)).run();
	}

}
