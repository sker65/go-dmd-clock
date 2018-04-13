package com.rinke.solutions.beans;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.beans.BeanFactory.PropertyProvider;
import com.rinke.solutions.pinball.util.Config;

@RunWith(MockitoJUnitRunner.class)
public class SimpleBeanFactoryTest {
	
	@Value(defaultValue="5")
	int testField;
	
	@Mock
	private PropertyProvider valueProvider;
	
	@InjectMocks
	private SimpleBeanFactory uut = new SimpleBeanFactory();

	@Before
	public void setUp() throws Exception {
	}

	@Test(expected=RuntimeException.class)
	public void testGetBeansOfType() throws Exception {
		uut.getBeanByType(String.class);
	}

	@Test(expected=RuntimeException.class)
	public void testGetBean() throws Exception {
		uut.getBean("foo");
	}

	@Test
	public void testGetBeanWithSingleton() throws Exception {
		uut.setSingleton("foo", new Object());
		uut.getBean("foo");
	}

	@Test
	public void testScanPackages() throws Exception {
		uut.scanPackages("com.rinke.solutions.pinball.util");
	}

	@Test
	public void testScanPackagesAndGetType() throws Exception {
		uut.scanPackages("com.rinke.solutions.pinball.util");
		uut.getBeansOfType(Config.class);
	}

	@Test
	public void testGetValueForField() throws Exception {
		Field f = this.getClass().getDeclaredField("testField");
		uut.getValueForField(f );
	}

}
