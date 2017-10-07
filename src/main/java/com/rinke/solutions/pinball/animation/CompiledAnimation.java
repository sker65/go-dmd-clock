package com.rinke.solutions.pinball.animation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bouncycastle.util.Arrays;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;

public class CompiledAnimation extends Animation {

	public List<Frame> frames;
	
	public CompiledAnimation(AnimationType type, String name, int start,
			int end, int skip, int cycles, int holdCycles) {
		this(type, name, start, end, skip, cycles, holdCycles, 128, 32);
	}
	
	public CompiledAnimation(AnimationType type, String name, int start,
			int end, int skip, int cycles, int holdCycles, int w, int h) {
		super(type, name, start, end, skip, cycles, holdCycles, w, h);
		init();
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

    // looks like there is a chance that commit gets called on an new (already switched) animation, while dmd
    // content still has the content of the old (that was displayed before)
	@Override
	public void commitDMDchanges(DMD dmd, byte[] hash) {
		if( clockWasAdded ) {		// never commit a frame were clock was rendered, this is savety check only
			clockWasAdded = false;
			return;
		}
	    if( actFrame >= 0 && actFrame < frames.size() ) {
	    	Frame aniFrame = frames.get(actFrame);
	    	if( dmd.canUndo() ) {
		        Frame dmdFrame = dmd.getFrame();
		        // if target has no mask, don't create one
		        dmdFrame.copyToWithMask(aniFrame, aniFrame.hasMask() ? -1 : (-1<<1) );
		        setDirty(true);
	    	}
	    	if( !Arrays.areEqual(hash, aniFrame.crc32)) {
		        aniFrame.setHash(hash);
		        setDirty(true);
	    	}
	    }
	}

	public static List<Animation> read(String file) {
		return AniReader.readFromFile(file);
	}

	public static void write(List<Animation> anis, String filename, int version, Map<Integer,Palette> palettes) {
		AniWriter.writeToFile(anis, filename, version, palettes);
	}

	/**
	 * ensure every frame in this animation has a mask plane
	 * TODO not ok from goDMD / transitions
	 */
	public void ensureMask() {
		for(Frame frame : frames) {
			if(!frame.hasMask()) {
				byte[] data = new byte[frame.getPlane(0).length];
				Arrays.fill(data, (byte)0xFF);
				frame.setMask(data);
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




}
