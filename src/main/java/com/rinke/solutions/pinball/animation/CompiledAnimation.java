package com.rinke.solutions.pinball.animation;

import java.util.List;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;

public class CompiledAnimation extends Animation {

	public List<Frame> frames;
	Frame last = null;
	
	public CompiledAnimation(AnimationType type, String name, int start,
			int end, int skip, int cycles, int holdCycles) {
		super(type, name, start, end, skip, cycles, holdCycles);
		init();
		frames = renderer.getFrames();
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
	    int frameNo = actFrame -1;
	    if( frameNo<0 )frameNo = 0;
	    if( frameNo > frames.size()-1 ) frameNo = frames.size()-1;
		int r = frames.get(frameNo).delay;
		return r==0?super.getRefreshDelay():r;
	}

    @Override
    public int getFrameCount(DMD dmd) {
        return frames.size();
    }

    @Override
    protected Frame addTransitionFrame(Frame in) {
        return in;
    }

	@Override
	public void commitDMDchanges(DMD dmd) {
	    if( actFrame >= 0 && actFrame < frames.size()) {
	        List<Plane> planes = frames.get(actFrame).planes;
	        List<byte[]> dmdPlanes = dmd.getActualBuffers();
	        for(int i=0; i<planes.size(); i++) {
	            int len = min(dmdPlanes.get(i).length,planes.get(i).plane.length);
	            System.arraycopy(dmdPlanes.get(i), 0, planes.get(i).plane, 0, len );
	        }
	    }
	}

	private int min(int l, int l2) {
		return l<l2?l:l2;
	}
	
}
