package com.rinke.solutions.pinball.renderer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Properties;

import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.pinball.DMD;
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
	
	void readImage(String filename, DMD dmd){
		
	}

	// override if renderer wants to implement progress dialog
	void readImage(String filename, DMD dmd, Shell shell) {
		readImage(filename, dmd);
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
	
	protected Frame convertToFrameWithPalette(BufferedImage dmdImage, DMD dmd, Palette palette) {
		Frame res = new Frame();
		int noOfPlanes = 1;
		while( palette.numberOfColors > (1<<noOfPlanes)) noOfPlanes++;
		for( int j = 0; j < noOfPlanes ; j++) {
			res.planes.add(new Plane((byte)j, new byte[dmd.getFrameSizeInByte()]));
		}
		
		for (int x = 0; x < dmd.getWidth(); x++) {
			for (int y = 0; y < dmd.getHeight(); y++) {

				int rgb = dmdImage.getRGB(x, y);
				int idx = findColorIndex(rgb, palette);
				
				for( int j = 0; j < noOfPlanes ; j++) {
					if( (idx & (1<<j)) != 0)
						res.planes.get(j).plane[y * dmd.getBytesPerRow() + x / 8] |= (128 >> (x % 8));
				}

			}
		}
		return res;
	}
	
	private int findColorIndex(int rgb, Palette palette) {
		for (int i = 0; i < palette.colors.length; i++) {
			RGB p = palette.colors[i];
			if( p.red == ((rgb >> 16) & 0xFF) && p.green == ((rgb >> 8) & 0xFF) && p.blue == (rgb & 0xFF) ) {
				return i;
			}
			
		}
		return 0;
	}

	protected Frame convertToFrame(BufferedImage dmdImage, DMD dmd) {
		Frame res = new Frame();
		for( int j = 0; j < 15 ; j++) {
			res.planes.add(new Plane((byte)j, new byte[dmd.getFrameSizeInByte()]));
		}
		
		for (int x = 0; x < dmd.getWidth(); x++) {
			for (int y = 0; y < dmd.getHeight(); y++) {

				int rgb = dmdImage.getRGB(x, y);
				
				// reduce color depth to 15 bit
				int nrgb = ( rgb >> 3 ) & 0x1F;
				nrgb |= ( ( rgb >> 11 ) & 0x1F ) << 5;
				nrgb |= ( ( rgb >> 19 ) & 0x1F ) << 10;
				
				for( int j = 0; j < 15 ; j++) {
					if( (nrgb & (1<<j)) != 0)
						res.planes.get(j).plane[y * dmd.getBytesPerRow() + x / 8] |= (128 >> (x % 8));
				}

			}
		}
		return res;
	}

	public Palette getPalette() {
		return palette;
	}



}
