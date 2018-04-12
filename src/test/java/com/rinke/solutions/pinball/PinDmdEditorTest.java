package com.rinke.solutions.pinball;

import java.beans.PropertyChangeEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

import com.rinke.solutions.beans.BeanFactory;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.view.ChangeHandlerHelper;
import com.rinke.solutions.pinball.view.handler.AutosaveHandler;
import com.rinke.solutions.pinball.view.handler.MenuHandler;
import com.rinke.solutions.pinball.view.handler.ViewBindingHandler;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class PinDmdEditorTest {
	
	@Mock BeanFactory beanFactory;
	@Mock ChangeHandlerHelper<ViewBindingHandler> viewModelChangeHandler;
	@Mock AutosaveHandler autoSaveHandler;
	@Mock MenuHandler menuHandler;
	@Mock AnimationHandler animationHandler;
	
	@InjectMocks
	PinDmdEditor uut = new PinDmdEditor();
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testViewModelChanged() throws Exception {
		PropertyChangeEvent e = new PropertyChangeEvent(uut, "foo", 1, 2);
		uut.viewModelChanged(e );
	}

	@Test
	public void testProcessCmdLine() throws Exception {
		String[] args = new String[]{};
		uut.parseCmdLine(args );
	}

	@Test
	public void testOpen() throws Exception {
		String[] args = new String[]{};
		
		uut.beanFactory = mock(BeanFactory.class);
		uut.mainView = mock(MainView.class);
		Config config = mock(Config.class);
		
		ViewModel vm = new ViewModel();
		
		when( uut.beanFactory.getBeanByType(Config.class) ).thenReturn(config);
		when( uut.beanFactory.getBeanByType(ViewModel.class) ) .thenReturn(vm );
		when( config.getInteger(Config.DMDSIZE,0)).thenReturn(0);
		
		uut.open(args);
	}

	@Test
	public void testCheckForPlugins() throws Exception {
		uut.checkForPlugins();
	}

}
