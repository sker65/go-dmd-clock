package com.rinke.solutions.pinball.swt;

import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SWTDispatcherSWTTest {
	
	@Mock
	private Display display;
	
	@InjectMocks
	private SWTDispatcher uut;
	
	Runnable runner = new Runnable() {
		@Override
		public void run() {
		}
	};

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testAsyncExec() throws Exception {
		uut.asyncExec(runner);
	}

	@Test
	public void testTimerExec() throws Exception {
		uut.timerExec(1, runner);
	}

	@Test
	public void testSyncExec() throws Exception {
		uut.syncExec(runner);
	}

}
