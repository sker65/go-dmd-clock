package com.rinke.solutions.pinball;

import java.util.ArrayList;
import java.util.List;

public class CompiledAnimation extends Animation {

	List<Frame> frames = new ArrayList<>();
	Frame last = null;
	
	public CompiledAnimation(AnimationType type, String name, int start,
			int end, int skip, int cycles, int holdCycles) {
		super(type, name, start, end, skip, cycles, holdCycles);
	}

	@Override
	protected Frame renderFrame(String name, DMD dmd, int act) {
		if( act < frames.size()) {
			last = frames.get(act);//new Frame(dmd.getWidth(), dmd.getHeight(), frames.get(act).planes.get(0).plane, frames.get(act).planes.get(1).plane);
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
		int r = frames.get(actFrame<frames.size()?actFrame:frames.size()-1).delay;
		return r==0?super.getRefreshDelay():r;
	}

    @Override
    public int getFrameCount(DMD dmd) {
        return frames.size();
    }

    @Override
    public void setPixel( int x, int y) {
        if( last != null ) {
            last.setPixel(x,y);
        }
    }

}
