package com.rinke.solutions.pinball.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;

@Slf4j
public class CompiledAnimation extends Animation {

	public List<Frame> frames;
	private List<Mask> masks = new ArrayList<>();	// for layered coloring
	
	// for follow hash scenes
	public static class RecordingLink {
		public String associatedRecordingName;
		public int startFrame;
		public RecordingLink(String associatedRecordingName, int startFrame) {
			super();
			this.associatedRecordingName = associatedRecordingName;
			this.startFrame = startFrame;
		}
		@Override
		public String toString() {
			return String.format("RecordingLink [associatedRecordingName=%s, startFrame=%s]", associatedRecordingName, startFrame);
		}
	}
	
	@Getter @Setter
	private RecordingLink recordingLink;
	
	public CompiledAnimation(AnimationType type, String name, int start,
			int end, int skip, int cycles, int holdCycles) {
		this(type, name, start, end, skip, cycles, holdCycles, 128, 32);
	}
	
	public CompiledAnimation(AnimationType type, String name, int start,
			int end, int skip, int cycles, int holdCycles, int w, int h) {
		super(type, name, start, end, skip, cycles, holdCycles, w, h);
		init(this.progressEventListener);
		setMutable(true);
		frames = renderer.getFrames();
	}
	
	@Override
	public List<Mask> getMasks() {
		return masks;
	}
	
	public void lockMask(int i) {
		if( i >= 0 && i < masks.size() ) {
			masks.get(i).locked = true;
		}
	}

	public void unlockMask(int i) {
		if( i >= 0 && i < masks.size() ) {
			masks.get(i).locked = false;
		}
	}

	public Mask getMask(int i) {
		return getMaskWithSize(i, (width/8 * height));
	}
	
	public Mask getMaskWithSize(int i, int size) {
		while( i+1 > masks.size()) {
			Mask mask = new Mask(size);
			masks.add( mask );
		}
		Mask m = masks.get(i);
		if( m.data.length != size ) {
			// sanitize mask size
			m.data = Arrays.copyOfRange(m.data,0,size);
		}
		return m;
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
    public int getFrameCount(DMD dmd) {
        return frames.size();
    }

    @Override
    protected Frame addTransitionFrame(Frame in) {
        return in;
    }

    public Mask getCurrentMask() {
		// create mask for current frame on the fly if there is none		
		Frame frame = frames.get(actFrame);
		if( !frame.hasMask() ) {
			frame.setMask(new Mask(this.width*this.height/8));
		}
		return frame.mask;
    }

    // looks like there is a chance that commit gets called on an new (already switched) animation, while dmd
    // content still has the content of the old (that was displayed before)
	@Override
	public void commitDMDchanges(DMD dmd) {
		if( clockWasAdded ) {		// never commit a frame were clock was rendered, this is savety check only
			clockWasAdded = false;
			return;
		}
	    if( actFrame >= 0 && actFrame < frames.size() ) {
	    	Frame aniFrame = frames.get(actFrame);
	    	if( dmd.canUndo() ) {
		        Frame dmdFrame = dmd.getFrame();
		        // if target has no mask, don't create one
		        int copyMask = aniFrame.hasMask() ? -1 : (-1<<1);
		        log.debug("commitDMDchanges -> planes: {}, copyMask: {}", dmdFrame.planes.size(), Integer.toBinaryString(copyMask & 0xFFFF));
		        dmdFrame.copyToWithMask(aniFrame, copyMask );
		        setDirty(true);
	    	}
	    }
	}

	/**
	 * ensure every frame in this animation has a mask plane
	 * TODO not ok from goDMD / transitions
	 */
	public void ensureMask() {
		for(Frame frame : frames) {
			if(!frame.hasMask()) {
				frame.setMask(new Mask(frame.getPlane(0).length));
			}
		}
	}

	@Override
	public void setDimension(int width, int height) {
		super.setDimension(width, height);
		if( !frames.isEmpty() ) throw new RuntimeException("cannot set dimension, when already allocates frames");
	}
	
	public int getNumberOfPlanes() {
		return frames != null ? frames.stream().mapToInt(f->f.planes.size()).max().orElse(0) : 0;
	}

	/**
	 * convenience getter for actual frame
	 */
	public Frame getActualFrame() {
		return frames.get(actFrame);
	}
	
	public Frame getPreviousFrame() {
		if (actFrame > 0)
			return frames.get(actFrame-1);
		return frames.get(actFrame);
	}
	
	public Frame getNextFrame() {
		if (frames.size() > 1 && actFrame < frames.size()-1)
			return frames.get(actFrame+1);
		return frames.get(actFrame);
	}

}
