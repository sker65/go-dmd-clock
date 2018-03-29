package com.rinke.solutions.pinball.view.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.MainView;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class AutosaveHandlerTest extends HandlerTest {
	
	@Mock MainView mainView;
	@Mock ProjectHandler projectHandler;

	@InjectMocks
	private AutosaveHandler uut = new AutosaveHandler(vm);

	@Before
	public void setUp() throws Exception {
		uut.autosave = true;
	}

	@Test
	public void testDoAutoSave() throws Exception {
		uut.doAutoSave();
	}

	@Test
	public void testCheckAutoSaveAtStartup() throws Exception {
		uut.checkAutoSaveAtStartup();
	}

	@Test
	public void testOnAutoSave() throws Exception {
		uut.onAutoSave();
	}

	@Test
	public void testOnDeleteAutosaveFiles() throws Exception {
		uut.onDeleteAutosaveFiles();
	}

	@Test
	public void testRun() throws Exception {
		uut.run();
	}

}
