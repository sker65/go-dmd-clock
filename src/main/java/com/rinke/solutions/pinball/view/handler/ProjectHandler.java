package com.rinke.solutions.pinball.view.handler;

import java.util.List;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DMDClock;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.CmdDispatcher.Command;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
public class ProjectHandler extends ViewHandler {

	public ProjectHandler(ViewModel vm, Model model, CmdDispatcher d) {
		super(vm, model, d);
	}
	

}
