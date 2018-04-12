package com.rinke.solutions.pinball.view;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.beans.BeanFactory;

@RunWith(MockitoJUnitRunner.class)
public class ChangeHandlerHelperTest {
	@Mock
	private BeanFactory beanFactory;
	
	@InjectMocks
	ChangeHandlerHelper<String> uut = new ChangeHandlerHelper<>(beanFactory, String.class);

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCallOnChangedHandlers() throws Exception {
		Object nv = new Object();
		Object ov = new Object();
		uut.callOnChangedHandlers("foo", nv, ov);
	}

}
