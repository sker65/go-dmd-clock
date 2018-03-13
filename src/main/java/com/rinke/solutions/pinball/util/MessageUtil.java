package com.rinke.solutions.pinball.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;

@Bean
public class MessageUtil {
	
	@Autowired private Shell shell;
	
	public MessageUtil() {
		super();
	}

	public int warn(String header, String msg) {
		return warn(SWT.ICON_WARNING | SWT.OK, header, msg);
	}

	public int warn(int style, String header, String msg) {
		MessageBox messageBox = new MessageBox(shell, style);
		messageBox.setText(header);
		messageBox.setMessage(msg);
		return messageBox.open();
	}

	public void error(String header, String msg) {
		warn(SWT.ICON_ERROR | SWT.OK, header, msg);
	}

}