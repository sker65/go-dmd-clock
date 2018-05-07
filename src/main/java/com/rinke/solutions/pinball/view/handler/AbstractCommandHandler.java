package com.rinke.solutions.pinball.view.handler;

import com.rinke.solutions.pinball.view.model.ViewModel;

public class AbstractCommandHandler implements CommandHandler {
	
	protected ViewModel vm;
	
	public AbstractCommandHandler(ViewModel vm) {
		super();
		this.vm = vm;
	}

}
