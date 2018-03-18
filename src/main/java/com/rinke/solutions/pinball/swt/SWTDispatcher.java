package com.rinke.solutions.pinball.swt;

import org.eclipse.swt.widgets.Display;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.Dispatcher;

@Bean
public class SWTDispatcher implements Dispatcher {
	
	Display display;

	public SWTDispatcher() {
		super();
		this.display = Display.getCurrent();
	}
	
	/* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.swt.Dispatcher#asyncExec(java.lang.Runnable)
	 */
	@Override
	public void asyncExec( Runnable runnable ) {
		display.asyncExec(runnable);
	}
	
	/* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.swt.Dispatcher#timerExec(int, java.lang.Runnable)
	 */
	@Override
	public void timerExec( int milliseconds, Runnable runnable ) {
		display.timerExec(milliseconds, runnable);
	}

	/* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.swt.Dispatcher#syncExec(java.lang.Runnable)
	 */
	@Override
	public void syncExec( Runnable runnable ) {
		display.syncExec(runnable);
	}
	
}
