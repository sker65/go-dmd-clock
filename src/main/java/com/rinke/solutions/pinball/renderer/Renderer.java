package com.rinke.solutions.pinball.renderer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

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
	
	protected BufferedReader getReader(String filename) throws IOException {
		if( filename.endsWith(".gz")) {
			return new BufferedReader(
					new InputStreamReader(new GZIPInputStream(
							new FileInputStream(new File(filename)))));
		} else {
			return new BufferedReader(
					new InputStreamReader(
							new FileInputStream(new File(filename))));
		}
	}
	
	protected BufferedInputStream getInputStream(String filename) throws IOException {
		if( filename.endsWith(".gz")) {
			return new BufferedInputStream(new GZIPInputStream(
					new FileInputStream(new File(filename))));
		} else {
			return new BufferedInputStream(
					new FileInputStream(new File(filename)));
		}
	}

	protected String bareName(String filename) {
		if( filename == null ) return null;
		String b = new File(filename).getName();
		int i = b.lastIndexOf('.');
		return i==-1?b:b.substring(0, i);
	}

	public void innerRun() {
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
		if (getNumberOfPlanes() != 24)
			return palette;
		else
			return null;
	}



}
