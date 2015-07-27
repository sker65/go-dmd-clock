package com.rinke.solutions.pinball;

import java.util.ArrayList;
import java.util.List;

import com.rinke.solutions.pinball.renderer.FrameSet;

public class CompiledAnimation extends Animation {

	List<Frame> frames = new ArrayList<>();
	
	FrameSet last = null;
	
	public static class Frame {
		public Frame(int delay) {
			this.delay = delay;
		}
		int delay;
		List<Plane> planes = new ArrayList<>();
	}
	
	public static class Plane {
		byte marker;
		byte[] plane;
		public Plane(byte marker, byte[] plane) {
			super();
			this.marker = marker;
			this.plane = plane;
		}
	}
	
	public CompiledAnimation(AnimationType type, String name, int start,
			int end, int skip, int cycles, int holdCycles) {
		super(type, name, start, end, skip, cycles, holdCycles);
	}

	@Override
	protected FrameSet renderFrameSet(String name, DMD dmd, int act) {
		if( act < frames.size()) {
			last = new FrameSet(dmd.getWidth(), dmd.getHeight(), frames.get(act).planes.get(0).plane, frames.get(act).planes.get(1).plane);
		}
		return last;
	}

	public void addPlane( int act, Plane f1 ) {
		frames.get(act).planes.add(f1);
	}
	
	public void addFrame(Frame f) {
		frames.add(f);
		this.end++;
	}

	@Override
	public int getRefreshDelay() {
		return frames.get(actFrame<frames.size()?actFrame:frames.size()-1).delay;
	}

}
