package com.rinke.solutions.pinball.swt;

import org.eclipse.swt.widgets.Display;

import com.rinke.solutions.beans.Bean;

@Bean
public class SWTDispatcher {
	
	Display display;

	public SWTDispatcher(Display display) {
		super();
		this.display = display;
	}

	public void timerExec(int milliseconds, Runnable runnable) {
		display.timerExec(milliseconds, runnable);
	}

}
