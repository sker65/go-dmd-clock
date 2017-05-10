package com.rinke.solutions.pinball.view.handler;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
public class DrawingHandler extends ViewHandler {

	public DrawingHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm,m,d);
	}
	
	// observe palette changes
	// propaget to draw tool
	public void onSelectedPaletteChanged(Palette ov, Palette nv) {
		//vm.dmdWidget.setPalette ...
	}
	
}
