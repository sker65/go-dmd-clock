package com.rinke.solutions.pinball.view.handler;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.view.model.ViewModel;

public class HandlerTest {
	
	public HandlerTest() {
		super();
		vm.init(dmd, DmdSize.Size128x32, "address", 10);
	}

	protected ViewModel vm = new ViewModel();
	protected DMD dmd = new DMD(128,32);
	protected CompiledAnimation ani = new CompiledAnimation(AnimationType.COMPILED, "foo",0,0,0,0,0);

}
