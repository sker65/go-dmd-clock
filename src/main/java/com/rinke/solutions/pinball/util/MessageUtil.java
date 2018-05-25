package com.rinke.solutions.pinball.util;

import java.util.concurrent.Callable;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.Dispatcher;
import com.rinke.solutions.pinball.ui.CustomMessageBox;

@Bean
@Slf4j
public class MessageUtil {
	
	@Autowired private Shell shell;
	@Autowired private Dispatcher dispatcher;
	
	public MessageUtil() {
		super();
	}

	public int warn(String header, String msg) {
		return warn(SWT.ICON_WARNING | SWT.OK, header, msg);
	}

	public int warn(int style, String header, String msg) {
		CallableTask<Integer> ct = new CallableTask<Integer>( () -> {
			MessageBox messageBox = new MessageBox(shell, style);
			messageBox.setText(header);
			messageBox.setMessage(msg);
			return messageBox.open();			
		});
		dispatcher.syncExec(ct);
		return ct.getResult();
	}
	
	private static class CallableTask<V> implements Runnable, Callable<V> {

		Callable<V> delegate;
		
		public CallableTask(Callable<V> delegate) {
			super();
			this.delegate = delegate;
		}

		@Getter
		V result;
		
		@Override
		public V call() throws Exception {
			result = delegate.call();
			return result;
		}

		@Override
		public void run() {
			try {
				this.call();
			} catch (Exception e) {
				log.error("problem calling {}", this, e);
			}
		}
		
	}

	public int warn(int style, String title, String header, String msg, String[] buttons, int def) {
		CallableTask<Integer> ct = new CallableTask<Integer>( () -> {
			CustomMessageBox messageBox = new CustomMessageBox(shell, style, SWT.ICON_WARNING, title,header, msg, buttons,def);
			return messageBox.open();
		});
		
		dispatcher.syncExec(ct);
		return ct.getResult();
	}

	public void error(String header, String msg) {
		warn(SWT.ICON_ERROR | SWT.OK, header, msg);
	}

}