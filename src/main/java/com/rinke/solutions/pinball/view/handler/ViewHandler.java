package com.rinke.solutions.pinball.view.handler;

import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

public class ViewHandler {
	
	public final ViewModel vm;
	public final Model model;
	public final CmdDispatcher dispatcher;

	public ViewHandler(ViewModel vm, Model model, CmdDispatcher dispatcher) {
		super();
		this.vm = vm;
		this.model = model;
		this.dispatcher = dispatcher;
	}

}
