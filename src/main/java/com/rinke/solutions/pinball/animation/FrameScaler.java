package com.rinke.solutions.pinball.animation;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Plane;

public class FrameScaler {


	// scale by next pixel
	public static Frame scaleFrame(Frame in, int w, int h ) {
		int noOfPlanes = in.planes.size();
		int scaledPlaneSize = w * h * 4 / 8;
		// if( pal.numberOfColors == 16 ) 
		int bytesPerRow = w / 8;
		int scaledBytesPerRow = w / 4;
		int srcPlaneSize = w*h/8;
		Frame r = createFrame(in, scaledPlaneSize);
		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++) {
				int v = getPixel( in, x, y, bytesPerRow, srcPlaneSize);
				setPixel( r, v, x*2, y*2, scaledBytesPerRow);
				setPixel( r, v, x*2+1, y*2, scaledBytesPerRow);
			}
			// double row
			copyRow(r,y*2, y*2+1, scaledBytesPerRow);
		}

		return r;
	}
	
	public static Frame scale2xFrame(Frame in, int w, int h ) {
		int noOfPlanes = in.planes.size();
		int scaledPlaneSize = w * h * 4 / 8;
		//int sw = w*2;
		//int sh = h*2;
		// if( pal.numberOfColors == 16 ) 
		int bytesPerRow = w / 8;
		int scaledBytesPerRow = w / 4;
		int srcPlaneSize = w*h/8;
		Frame r = createFrame(in, scaledPlaneSize);
		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++) {
				int p = getPixel( in, x, y, bytesPerRow, srcPlaneSize);
				int p1 = p;
				int p2 = p;
				int p3 = p;
				int p4 = p;
				int a = getPixel( in, x, y-1, bytesPerRow, srcPlaneSize);
				int b = getPixel( in, x+1, y, bytesPerRow, srcPlaneSize);
				int c = getPixel( in, x-1, y, bytesPerRow, srcPlaneSize);
				int d = getPixel( in, x, y+1, bytesPerRow, srcPlaneSize);

				if( c == a && c!=d && a!= b ) p1 = a;
				if( a == b && a!=c && b!= d ) p2 = b;
				if( d == c && d!=b && c!= a ) p3 = c;
				if( b == d && b!=a && d!= c ) p4 = d;
				setPixel( r, p1, x*2, y*2, scaledBytesPerRow);
				setPixel( r, p2, x*2+1, y*2, scaledBytesPerRow);
				setPixel( r, p3, x*2, y*2+1, scaledBytesPerRow);
				setPixel( r, p4, x*2+1, y*2+1, scaledBytesPerRow);
			}
		}

		return r;
	}

	private static void copyRow(Frame r, int fromRow, int toRow, int bytesPerRow) {
		int numberOfPlanes = r.planes.size();
    	for(int plane = 0; plane < numberOfPlanes; plane++) {
    		for(int i = 0; i<bytesPerRow;i++) {
    			r.planes.get(plane).data[toRow*bytesPerRow+i] = r.planes.get(plane).data[fromRow*bytesPerRow+i];
    		}
    	}
	}

	private static void setPixel(Frame r, int v, int x, int y, int bytesPerRow) {
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

	private static int getPixel(Frame frame, int x, int y, int bytesPerRow, int planeSize) {
    	byte mask = (byte) (0b10000000 >> (x % 8));
    	int v = 0;
    	for(int plane = 0; plane < frame.planes.size(); plane++) {
    		int i = x / 8 + y * bytesPerRow;
    		if( i<0)i=0;
    		if(i>planeSize-1)i=planeSize-1;
    		v += (frame.planes.get(plane).data[i] & mask) != 0 ? (1<<plane) : 0;
    	}
    	return v;
	}

	private static Frame createFrame(Frame src, int planeSize) {
		Frame r = new Frame(src);
		int noOfPlanes = src.planes.size();
		r.planes.clear();
		for( int i = 0; i < noOfPlanes; i++) {
			r.planes.add(new Plane((byte)i, new byte[planeSize]));
		}
		r.mask = new Mask(planeSize);
		return r;
	}

}
