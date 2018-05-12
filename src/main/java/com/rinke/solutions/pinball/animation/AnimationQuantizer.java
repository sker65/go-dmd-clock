package com.rinke.solutions.pinball.animation;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;

public class AnimationQuantizer {

	public CompiledAnimation quantize(String name, CompiledAnimation in, Palette pal) {
		CompiledAnimation result = new CompiledAnimation(AnimationType.COMPILED, name, 
				in.start, in.end,in.skip, 0, 0, in.width, in.height);
		for( Frame inFrame : in.frames ) {
			Frame qFrame = quantizeFrame(inFrame, pal, in.width, in.height);
			qFrame.delay = inFrame.delay;
			qFrame.timecode = inFrame.timecode;
			qFrame.crc32 = inFrame.crc32;
			if( inFrame.mask != null ) qFrame.mask = new Plane(inFrame.mask);
			result.addFrame(qFrame);
		}
		return result;
	}
	
	private Frame quantizeFrame(Frame in, Palette pal, int w, int h) {
		int noOfPlanes = 4; // default
		int planeSize = in.planes.get(0).data.length;
		// if( pal.numberOfColors == 16 ) 
		int bytesPerRow = w / 8;
		Frame r = createFrame(noOfPlanes, planeSize);
		for( int x = 0; x < w; x++) {
			for( int y = 0; y < h; y++ ) {
				int rgb = getPixel( in, x, y, bytesPerRow);
				int idx = findBestColor( rgb, pal );
				setPixel( r, idx, x, y, bytesPerRow);
			}
		}
		
		return r;
	}

	private void setPixel(Frame r, int v, int x, int y, int bytesPerRow) {
    	byte mask = (byte) (0b10000000 >> (x % 8));
    	int numberOfPlanes = r.planes.size();
    	for(int plane = 0; plane < numberOfPlanes; plane++) {
    		if( (v & 0x01) != 0) {
    			r.planes.get(plane).data[y*bytesPerRow+x/8] |= mask;
    		} else {
    			r.planes.get(plane).data[y*bytesPerRow+x/8] &= ~mask;
    		}
    		v >>= 1;
    	}
	}

	private int findBestColor(int rgb, Palette pal) {
		for( int i = 0; i < pal.colors.length; i++) {
			if( pal.colors[i].red == rgb >> 16 
				&& pal.colors[i].green == ((rgb>>8) & 0xFF) 
				&& pal.colors[i].blue == (rgb & 0xFF) ) {
				return i; // exact color
			}
		}
		return findNearestColor(rgb, pal);
	}

	private int findNearestColor(int rgb, Palette pal) {
		double minDistance = Double.MAX_VALUE;
		int bestCol = 0;
		for( int i = 0; i < pal.colors.length; i++ ) {
			RGB c = pal.colors[i];
			double dist = getColorDistance(c.red, c.green, c.blue, rgb>>16, (rgb>>8) & 0xff, rgb & 0xff);
			if( dist < minDistance ) {
				minDistance = dist;
				bestCol = i;
			}
		}
		return bestCol;
	}

	private int getPixel(Frame frame, int x, int y, int bytesPerRow) {
    	byte mask = (byte) (0b10000000 >> (x % 8));
    	int v = 0;
    	for(int plane = 0; plane < frame.planes.size(); plane++) {
    		v += (frame.planes.get(plane).data[x / 8 + y * bytesPerRow] & mask) != 0 ? (1<<plane) : 0;
    	}
    	return v;
	}

	private Frame createFrame(int noOfPlanes, int planeSize) {
		Frame r = new Frame();
		for( int i = 0; i < noOfPlanes; i++) {
			r.planes.add(new Plane((byte)i, new byte[planeSize]));
		}
		return r;
	}

	/**
	 * Returns the color distance between color1 and color2
	 */
	public float getColorDistance(RGB c1, RGB c2) {
		return (float) getColorDistance(c1.red, c1.green, c1.blue, c2.red, c2.green, c2.blue);
	}

	public double getColorDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
		return Math.sqrt(Math.pow(r2 - r1, 2) + Math.pow(g2 - g1, 2) + Math.pow(b2 - b1, 2));
	}

}
