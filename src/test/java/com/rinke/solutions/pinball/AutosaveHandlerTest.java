package com.rinke.solutions.pinball;

import java.io.IOException;

import static org.mockito.Mockito.*;

import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.rinke.solutions.pinball.swt.SWTDispatcher;
import com.rinke.solutions.pinball.util.MessageUtil;

public class AutosaveHandlerTest {
	
	AutosaveHandler uut;
	PinDmdEditor editor;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	private SWTDispatcher dispatcherMock;
	private MessageUtil util;
	
	@Before
	public void setup() {
		editor = new PinDmdEditor();
		dispatcherMock = mock(SWTDispatcher.class);
		util = mock(MessageUtil.class);
		uut = new AutosaveHandler(util, dispatcherMock ){

			@Override
			String getFilename() {
				try {
					return testFolder.newFile("test.xml").getPath();
				} catch (IOException e) {
					return null;
				}
			}
			
		};
	}

	@Test
	public void testRun() throws Exception {
		uut.run();
		verify(dispatcherMock).timerExec(eq(300000), eq(uut));
	}

	@Test
	public void testCheckAutoSaveAtStartup() throws Exception {
		//uut.checkAutoSaveAtStartup();
	}

	@Test
	public void testDeleteAutosaveFiles() throws Exception {
		
	}

}
