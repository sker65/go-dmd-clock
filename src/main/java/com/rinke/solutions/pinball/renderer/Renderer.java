package com.rinke.solutions.pinball.renderer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Properties;

import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.Worker;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;

public abstract class Renderer extends Worker {

	protected int lowThreshold = 50;
	protected int midThreshold = 120;
	protected int highThreshold = 200;
	protected int maxFrame = 0;
	protected Properties props = new Properties();

	List<Frame> frames = new ArrayList<>();
	protected Palette palette = null;
	
	protected String bareName(String filename) {
		if( filename == null ) return null;
		String b = new File(filename).getName();
		int i = b.lastIndexOf('.');
		return i==-1?b:b.substring(0, i);
	}

	public void run() {
	}

	public Frame convert(String filename, DMD dmd, int frameNo) {
		if (frames.isEmpty()) {
			readImage(filename, dmd);
		}
		return frames.get(frameNo);
	}

	public int getMaxFrame(String filename, DMD dmd) {
		if (frames.isEmpty()) readImage(filename, dmd);
		return maxFrame;
	}
	
	void readImage(String filename, DMD dmd){};
	
	public long getTimeCode(int actFrame) {
        return 0L;
    }
	
	public void setLowThreshold(int lowThreshold) {
		this.lowThreshold = lowThreshold;
	}

	public void setMidThreshold(int midThreshold) {
		this.midThreshold = midThreshold;
	}

	public void setHighThreshold(int highThreshold) {
		this.highThreshold = highThreshold;
	}

	public List<Frame> getFrames() {
		return frames;
	}

	public int getNumberOfPlanes() {
		OptionalInt optionalInt = frames.stream().mapToInt(f->f.planes.size()).max();
		return optionalInt.orElse(2);
	}

	public Properties getProps() {
		return props;
	}

	public void setProps(Properties props) {
		this.props = props;
	}
	
	public Palette getPalette() {
		return palette;
	}



}
