package com.rinke.solutions.beans;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

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

	@Test
	public void testGetBeansOfTypeWithSingleton() throws Exception {
		uut.setSingleton("foo", new String("foo"));
		uut.getBeansOfType(String.class);
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

	@Test
	public void testGetBeannameFromMethodName() throws Exception {
		assertEquals("foo",uut.getBeannameFromMethodName("getFoo"));
		assertEquals("foo",uut.getBeannameFromMethodName("createFoo"));
	}
	
	@Test
	public void testStartElement() throws Exception {
		AttributesImpl attributes = new AttributesImpl();
		attributes.addAttribute("uri", "locl", "id", "string", "foo");
		attributes.addAttribute("uri", "locl", "class", "string", "java.lang.Integer");
		attributes.addAttribute("uri", "locl", "value", "string", "1");
		uut.startElement("uri", "foo", "bean", attributes );
	}
	
	public static class TestObject {
		@Autowired public Integer foo;
	}

	@Test
	public void testInject() throws Exception {
		TestObject obj = new TestObject();
		uut.setSingleton("foo", Integer.valueOf(1));
		uut.inject(obj);
		assertEquals(Integer.valueOf(1), obj.foo);
	}

}
