package com.rinke.solutions.pinball.renderer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Properties;

import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.Worker;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;

public abstract class Renderer extends Worker {

	protected int lowThreshold = 50;
	protected int midThreshold = 120;
	protected int highThreshold = 200;
	protected int maxFrame = 0;
	protected Properties props = new Properties();

	List<Frame> frames = new ArrayList<>();
	protected Palette palette = null;
	
	public void run() {
	}

	public Frame convert(String filename, DMD dmd, int frameNo) {
		return convert(filename, dmd, frameNo, null);
	}
	
	public Frame convert(String filename, DMD dmd, int frameNo, Shell shell) {
		if (frames.isEmpty()) {
			readImage(filename, dmd, shell);
		}
		return frames.get(frameNo);
	}

	public int getMaxFrame(String filename, DMD dmd) {
		if (frames.isEmpty()) readImage(filename, dmd);
		return maxFrame;
	}
	
	// override if renderer wants to implement progress dialog
	void readImage(String filename, DMD dmd, Shell shell) {
		readImage(filename, dmd);
	}
	
	void readImage(String filename, DMD dmd){
		
	}
	
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
