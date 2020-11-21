package com.rinke.solutions.pinball.view.handler;

import com.rinke.solutions.pinball.Worker;
import com.rinke.solutions.pinball.ui.IProgress;

public class MockProgress implements IProgress{
	
	@Override
	public void notify(ProgressEvent evt) {
		
	}
	
	@Override
	public void setText(String string) {
		
	}
	
	@Override
	public void open(Worker w) {
		w.run();
	}

}
