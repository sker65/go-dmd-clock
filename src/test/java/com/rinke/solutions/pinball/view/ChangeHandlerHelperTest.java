package com.rinke.solutions.pinball.view;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Arrays;

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
	ChangeHandlerHelper<TestObject> uut = new ChangeHandlerHelper<>(beanFactory, TestObject.class);

	TestObject obj;
	
	@Before
	public void setUp() throws Exception {
		obj = new TestObject();
	}
	
	public static class TestObject {
		int foo;
		Long bar;
		boolean bool;
		public void onFooChanged(int o, int n) {
			foo = n;
		}
		public void onBoolChanged(boolean o, boolean n) {
			bool = n;
		}
		public void onBarChanged(Long o, Long n) {
			bar = n;
		}
	}

	@Test
	public void testCallOnChangedHandlers() throws Exception {
		when( beanFactory.getBeansOfType(TestObject.class) ).thenReturn(Arrays.asList(obj));
		uut.callOnChangedHandlers("foo", 1, 2);
		assertEquals(1, obj.foo);
		uut.callOnChangedHandlers("foo", 2, 3);
		assertEquals(2,  obj.foo);
		uut.callOnChangedHandlers("bar", 2L, 3L);
		assertEquals(Long.valueOf(2L),  obj.bar);
		uut.callOnChangedHandlers("bar", Long.valueOf(3L), Long.valueOf(2L));
		assertEquals(Long.valueOf(3L),  obj.bar);
		uut.callOnChangedHandlers("bool", true, false);
		assertEquals(true,  obj.bool);
	}

	@Test
	public void testCallOnChangedHandlersCache() throws Exception {
		Object nv = new Object();
		Object ov = new Object();
		uut.callOnChangedHandlers("foo", nv, ov);
		uut.callOnChangedHandlers("foo", nv, ov);
	}

}
