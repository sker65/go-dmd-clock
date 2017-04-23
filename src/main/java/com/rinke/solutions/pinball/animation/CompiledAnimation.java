package com.rinke.solutions.pinball.animation;

import java.util.List;
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
		super(type, name, start, end, skip, cycles, holdCycles);
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
		this.end = frames.size()-1;
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
			byte[] emptyMask = new byte[PinDmdEditor.PLANE_SIZE];
			Arrays.fill(emptyMask, (byte)0xFF);
			frame.setMask(emptyMask);
		}
		maskToUse = new Mask(frame.mask.plane, false);
		return maskToUse;
    }

	@Override
	public void commitDMDchanges(DMD dmd, byte[] hash) {
		if( clockWasAdded ) {		// never commit a frame were clock was rendered, this is savety check only
			clockWasAdded = false;
			return;
		}
	    if( actFrame >= 0 && actFrame < frames.size()) {
	    	Frame aniFrame = frames.get(actFrame);
	        List<Plane> aniPlanes = aniFrame.planes;
	        Frame dmdFrame = dmd.getFrame();
	        // if both have mask commit mask
	        if( aniFrame.hasMask() && dmd.hasMask() ) {
	        	int len = min(dmdFrame.mask.plane.length,aniFrame.mask.plane.length);
	        	System.arraycopy(dmdFrame.mask.plane, 0, aniFrame.mask.plane, 0, len );
	        }
	        // then commit (copy) planes
	        for(int i = 0; i < aniPlanes.size(); i++) {
	        	byte[] planeBytes = dmdFrame.getPlane(i);
	            int len = min(planeBytes.length,aniPlanes.get(i).plane.length);
	            System.arraycopy(planeBytes, 0, aniPlanes.get(i).plane, 0, len );
	        }
	        aniFrame.setHash(hash);
	        setDirty(true);
	    }
	}

	private int min(int l, int l2) {
		return l<l2?l:l2;
	}

	public static List<Animation> read(String file) {
		return AniReader.readFromFile(file);
	}

	public static void write(List<Animation> anis, String filename, int version, List<Palette> palettes) {
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
}
