package com.rinke.solutions.pinball.view.handler;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
public class ExportHandler extends ViewHandler {

	public ExportHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm,m,d);
	}
	
}
