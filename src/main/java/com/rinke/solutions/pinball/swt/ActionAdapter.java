package com.rinke.solutions.pinball.swt;

import org.eclipse.jface.action.Action;

public class ActionAdapter extends Action implements Runnable {

	private Runnable delegate;

	public ActionAdapter(Runnable delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public void run() {
		delegate.run();
	}

}
