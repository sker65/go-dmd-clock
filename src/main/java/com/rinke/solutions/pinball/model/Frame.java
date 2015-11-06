package com.rinke.solutions.pinball.model;

import java.util.List;

public class Frame {
	public int repeatCount;
	public List<byte[]> planes;
	
	public Frame(int repeatCount, List<byte[]> planes) {
		super();
		this.repeatCount = repeatCount;
		this.planes = planes;
	}

	@Override
	public String toString() {
		return "Frame [repeatCount=" + repeatCount + ", planes=" + planes + "]";
	}
	
}
