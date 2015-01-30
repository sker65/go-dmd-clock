package com.rinke.solutions.pinball;

import java.util.ArrayList;
import java.util.List;

import com.rinke.solutions.pinball.renderer.FrameSet;

public class CompiledAnimation extends Animation {

	int numberOfFrameSets;
	List<byte[]> frames1 = new ArrayList<>();
	List<byte[]> frames2 = new ArrayList<>();
	
	public CompiledAnimation(AnimationType type, String name, int start,
			int end, int skip, int cycles, int holdCycles) {
		super(type, name, start, end, skip, cycles, holdCycles);
	}

	@Override
	protected FrameSet renderFrameSet(String name, DMD dmd, int act) {
		return new FrameSet(dmd.getWidth(), dmd.getHeight(), frames1.get(act), frames2.get(act));
	}

	public void addFrames( byte[] f1, byte[] f2) {
		frames1.add(f1);
		frames2.add(f2);
		this.end++;
	}

}
