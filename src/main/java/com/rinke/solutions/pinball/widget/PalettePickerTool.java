package com.rinke.solutions.pinball.widget;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.CmdDispatcher.Command;

public class PalettePickerTool extends DrawTool {

	@Autowired CmdDispatcher dispatcher;
	
	public PalettePickerTool(int actualColor) {
		super(actualColor);
	}

	@Override
	public boolean mouseDown(int x, int y) {
		int rgb = dmd.getPixelWithoutMask(x, y);
		dispatcher.dispatch(new Command<Integer>(rgb, "pickColor"));
		return true;
	}
	
}
