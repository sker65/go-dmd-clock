package com.rinke.solutions.pinball.view.handler;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationActionHandler;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class ProjectHandlerTest {
	@Mock
	private FileChooserUtil fileChooserUtil;

	@Mock
	private MessageUtil messageUtil;
	
	@InjectMocks
	private ProjectHandler uut;
	
	@Before public void setup() {
		// maybe with bean factory
		uut.fileHelper = new FileHelper();
		uut.vm = new ViewModel();
		uut.aniAction = new AnimationActionHandler();
		uut.aniAction.setVm(uut.vm);
	}

	@Test
	public void testBareName() throws Exception {
		assertEquals("foo", uut.bareName("foo"));
		assertEquals("foo", uut.bareName("foo.xml"));
		assertEquals("foo", uut.bareName("/USER/foo.xml"));
	}
	
	@Test
	public final void testLoadProject() {
		uut.onLoadProject("src/test/resources/ex1.xml");
	}



}
