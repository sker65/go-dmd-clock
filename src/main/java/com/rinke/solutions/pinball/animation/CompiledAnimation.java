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

    private Optional<Plane> searchMaskPlane(Frame frame) {
		return frame.planes.stream().filter(p->p.marker == Plane.MASK).findFirst();
	}

    public Mask getCurrentMask() {
		Mask maskToUse;
		// build mask from current frame
		
		Frame frame = frames.get(actFrame);
		Optional<Plane> maskPlane = searchMaskPlane(frame);
		if( maskPlane.isPresent() ) {
			maskToUse = new Mask(maskPlane.get().plane, false);
		} else {
			byte[] emptyMask = new byte[PinDmdEditor.PLANE_SIZE];
			Arrays.fill(emptyMask, (byte)0xFF);
			Plane masPlane = new Plane(Plane.MASK, emptyMask);
			frame.planes.add(0, masPlane);
			maskToUse = new Mask(masPlane.plane, false);
		}
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
	        List<Plane> planes = aniFrame.planes;
	        Frame dmdFrame = dmd.getFrame();
	        int i = 0;
	        if( aniFrame.containsMask() && dmd.hasMask() ) {
	        	byte[] planeBytes = dmd.getFrame().getPlaneBytes(0);
	        	int len = min(planeBytes.length,planes.get(i).plane.length);
	        	System.arraycopy(planeBytes, 0, planes.get(i).plane, 0, len );
	        	i++;
	        }
	        for(; i<planes.size(); i++) {
	        	byte[] planeBytes = dmdFrame.getPlaneBytes(i);
	            int len = min(planeBytes.length,planes.get(i).plane.length);
	            System.arraycopy(planeBytes, 0, planes.get(i).plane, 0, len );
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
}
