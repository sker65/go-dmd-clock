package com.rinke.solutions.pinball.renderer;

import com.rinke.solutions.pinball.DMD;

public abstract class Renderer {

	protected int lowThreshold = 50;
	protected int midThreshold = 120;
	protected int highThreshold = 200;
	protected int maxFrame = 0;

	public abstract FrameSet convert(String filename, DMD dmd, int frameNo);

	public int getMaxFrame() {
		return maxFrame;
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
