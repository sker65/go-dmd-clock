package com.rinke.solutions.beans;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.PinDmdEditor;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class SimpleBeanFactoryTest {
	
	BeanFactory uut;
	
	@Before
	public void setup() {
		uut = new SimpleBeanFactory();
		Display displayMock = mock(Display.class);
		Shell shell = mock(Shell.class);
		uut.setSingleton("display", displayMock);
		uut.setSingleton("shell", shell);
		uut.setSingleton("editor", mock(PinDmdEditor.class));
	}

	@Test
	public void testScanPackages() throws Exception {
		uut.scanPackages("com.rinke.solutions.pinball");
		Object bean = uut.getBean("com.rinke.solutions.pinball.view.handler.RecordingsHandler");
		assertNotNull(bean);
	}

}
