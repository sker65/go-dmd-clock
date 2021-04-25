package com.rinke.solutions.pinball.view.handler;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.view.model.ViewModel;

public class HandlerTest {
	
	public HandlerTest() {
		super();
		vm.init(dmd, DmdSize.Size128x32, "address", 10, new Config());
	}

	protected ViewModel vm = new ViewModel();
	protected DMD dmd = new DMD(DmdSize.Size128x32);
	protected CompiledAnimation ani = new CompiledAnimation(AnimationType.COMPILED, "foo",0,0,0,0,0);

	protected CompiledAnimation getScene(String name) {
		String n = name!=null?name:"scene";
		CompiledAnimation r = new CompiledAnimation(AnimationType.COMPILED, n, 0, 0, 0, 0, 0);
		Frame frame = new Frame(new byte[vm.dmdSize.planeSize], new byte[vm.dmdSize.planeSize]);
		r.frames.add(frame);
		r.frames.add(new Frame(frame));
		r.setDesc(n);
		return r;
	}
	
	protected byte[] getHash() {
		return new byte[]{1,2,3,4};
	}

}
