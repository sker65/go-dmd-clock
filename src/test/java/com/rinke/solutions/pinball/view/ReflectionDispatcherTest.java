package com.rinke.solutions.pinball.view;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.handler.CommandHandler;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class ReflectionDispatcherTest {
	
	@Mock
	private MessageUtil messageUtil;

	private ViewModel viewModel = new ViewModel();
	
	@InjectMocks
	private ReflectionDispatcher uut;
	
	public static class TestHandler implements CommandHandler {
		public void onBar(String foo) {
			System.out.println("hello "+foo);
		}

		public void onBar2(String foo) {
			System.out.println("hello "+foo);
		}

		public void onBar3(String foo, String doo) {
			System.out.println("hello "+foo);
		}
		
		public void onFoo() {
			System.out.println("foo");
		}

		public void onDelayChanged(int oldValue, int newValue) {
			System.out.println("old: "+oldValue+", new: "+newValue);
		}
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testRegisterHandler() throws Exception {
		uut.registerHandler(new TestHandler() );
	}

	@Test
	public void testDispatch() throws Exception {
		uut.registerHandler(new TestHandler() );
		uut.dispatch(new CmdDispatcher.Command<String>("foo", "bar"));
		uut.dispatch(new CmdDispatcher.Command<String>("foo", "bar2"));
		uut.dispatch(new CmdDispatcher.Command<String>("foo", "bar2"));
		uut.dispatch(new CmdDispatcher.Command<String[]>(new String[]{"foo", "doo"}, "bar3"));
		// check cache
		uut.dispatch(new CmdDispatcher.Command<String[]>(new String[]{"foo", "doo"}, "bar3"));
		uut.dispatch(new CmdDispatcher.Command<Object>(null, "foo"));
	}

}
