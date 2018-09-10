package com.rinke.solutions.pinball.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.bouncycastle.util.Arrays;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.renderer.Renderer;
import com.rinke.solutions.pinball.renderer.VPinMameRawRenderer;

public class RawAnimation extends Animation {

	public List<Frame> frames;
	public List<Plane> planes;
	
	public RawAnimation(AnimationType type, String name, int start,
			int end, int skip, int cycles, int holdCycles) {
		this(type, name, start, end, skip, cycles, holdCycles, 128, 32);
	}
	
	public RawAnimation(AnimationType type, String name, int start,
			int end, int skip, int cycles, int holdCycles, int w, int h) {
		super(type, name, start, end, skip, cycles, holdCycles, w, h);
		init(this.progressEventListener);
		setMutable(true);
		frames = renderer.getFrames();
	}
	
	@Override
	protected Frame renderFrame(String name, DMD dmd, int act) {
		if( act < frames.size()) {
			last = frames.get(act);//new Frame(dmd.getWidth(), dmd.getHeight(), frames.get(act).planes.get(0).plane, frames.get(act).planes.get(1).plane);
		}
		return last;
	}

	public void addFrame(Frame f) {
		frames.add(f);
		end = frames.size()-1;
	}

	public void addFrame(int pos, Frame f) {
		frames.add(pos,f);
		end = frames.size()-1;
	}

	public void removeFrame(int pos) {
		frames.remove(pos);
		end = frames.size()-1;
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
	protected void postInit(Renderer r) {
		if( r instanceof VPinMameRawRenderer ) {
			VPinMameRawRenderer renderer = (VPinMameRawRenderer)r;
			planes = renderer.getPlanes();
		}
	}

    @Override
    public int getFrameCount(DMD dmd) {
        return frames.size();
    }

    @Override
    protected Frame addTransitionFrame(Frame in) {
        return in;
    }

    public Mask getCurrentMask() {
		Mask maskToUse;
		// build mask from current frame		
		Frame frame = frames.get(actFrame);
		if( !frame.hasMask() ) {
			byte[] emptyMask = new byte[this.width*this.height/8];
			Arrays.fill(emptyMask, (byte)0xFF);
			frame.setMask(emptyMask);
		}
		maskToUse = new Mask(frame.mask.data, false);
		return maskToUse;
    }

	@Override
	public void setDimension(int width, int height) {
		super.setDimension(width, height);
		if( !frames.isEmpty() ) throw new RuntimeException("cannot set dimension, when already allocates frames");
	}
	
	public int getNumberOfPlanes() {
		return frames != null ? frames.stream().mapToInt(f->f.planes.size()).max().orElse(0) : 0;
	}

}
