package com.rinke.solutions.pinball.animation;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Optional;

import org.bouncycastle.util.Arrays;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.view.model.PropertyChangeSupported;

public class CompiledAnimation extends Animation implements PropertyChangeSupported {

	public List<Frame> frames;
	
	PropertyChangeSupport change = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener l) {
		change.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		change.removePropertyChangeListener(l);
	}

	private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		change.firePropertyChange(propertyName, oldValue, newValue);
	}

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
			byte[] emptyMask = new byte[this.width*this.height/8];
			Arrays.fill(emptyMask, (byte)0xFF);
			frame.setMask(emptyMask);
		}
		maskToUse = new Mask(frame.mask.data, false);
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
	        Frame dmdFrame = dmd.getFrame();
	        // if target has no mask, don't create one
	        dmdFrame.copyToWithMask(aniFrame, aniFrame.hasMask() ? -1 : (-1<<1) );
	        aniFrame.setHash(hash);
	        setDirty(true);
	    }
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

	@Override
	public void setDimension(int width, int height) {
		super.setDimension(width, height);
		if( !frames.isEmpty() ) throw new RuntimeException("cannot set dimension, when already allocates frames");
	}

	@Override
	public void setEditMode(EditMode editMode) {
		firePropertyChange("editMode", this.editMode, editMode);
		super.setEditMode(editMode);
	}

}
