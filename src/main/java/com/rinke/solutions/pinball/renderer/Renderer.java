package com.rinke.solutions.pinball.renderer;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.Frame;

public abstract class Renderer {

	protected int lowThreshold = 50;
	protected int midThreshold = 120;
	protected int highThreshold = 200;
	protected int maxFrame = 0;

	public abstract Frame convert(String filename, DMD dmd, int frameNo);

	public int getMaxFrame() {
		return maxFrame;
	}
	
	public long getTimeCode(int actFrame) {
        return 0L;
    }
	
	public void setLowThreshold(int lowThreshold) {
		this.lowThreshold = lowThreshold;
	}

	public void setMidThreshold(int midThreshold) {
		this.midThreshold = midThreshold;
	}

	public void setHighThreshold(int highThreshold) {
		this.highThreshold = highThreshold;
	}

}
