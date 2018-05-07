package com.rinke.solutions.pinball.view.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.AnimationHandler;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.ui.GifExporter;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.view.View;

@RunWith(MockitoJUnitRunner.class)
public class MenuHandlerTest extends HandlerTest  {

	@Mock GifExporter gifExporter;
	@Mock View about;
	@Mock AnimationHandler animationHandler;
	@Mock Config config;
	
	@InjectMocks MenuHandler uut = new MenuHandler(vm);
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testOnExportGif() throws Exception {
		vm.playingAnis.add(getScene("nam"));
		uut.onExportGif();
	}

	@Test
	public void testOnDmdSizeChanged() throws Exception {
		uut.onDmdSizeChanged(DmdSize.Size128x32, DmdSize.Size192x64);
	}

	@Test
	public void testOnNewProject() throws Exception {
		uut.onNewProject();
	}

	@Test
	public void testOnAbout() throws Exception {
		uut.onAbout();
	}

}
