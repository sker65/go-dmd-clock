package com.rinke.solutions.pinball.swt;

import org.eclipse.swt.widgets.Display;

import com.rinke.solutions.beans.Bean;

@Bean
public class SWTDispatcher implements TimerExec {
	
	Display display;

	public SWTDispatcher(Display display) {
		super();
		this.display = display;
	}

	/* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.swt.TimerExec#timerExec(int, java.lang.Runnable)
	 */
	@Override
	public void exec(int milliseconds, Runnable runnable) {
		display.timerExec(milliseconds, runnable);
	}

}
