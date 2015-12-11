package com.rinke.solutions.pinball;

import java.awt.Graphics2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Point;

import com.rinke.solutions.pinball.animation.Frame;

public class DMD {

    private int width;
    private int height;
    
    // TODO remove public reference to ensure the actual buffer are accessed
    public List<byte[]> frames = new ArrayList<byte[]>();
    
    public Map<Integer,List<byte[]>> buffers = new HashMap<Integer,List<byte[]>>();

    // remove simple fixed frames completely
    public byte[] frame1 = null;
    public byte[] frame2 = null;
    
    int numberOfSubframes = 2;
    public int actualBuffer = 0;

    private int frameSizeInByte;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFrameSizeInByte() {
        return frameSizeInByte;
    }

    private int bytesPerRow;

    public int getBytesPerRow() {
        return bytesPerRow;
    }

    public void copyTemp() {
    	List<byte[]> target = buffers.get(actualBuffer);
    	List<byte[]> source = buffers.get(actualBuffer-1);
    	for(int i = 0; i < source.size(); i++) {
    		System.arraycopy(source.get(i), 0, target.get(i), 0, source.get(i).length);
    	}
    }
    
    public void createTemp() {
    	List<byte[]> newframes = new ArrayList<byte[]>();
    	for( byte[] frame : frames) {
    		newframes.add(Arrays.copyOf(frame, frame.length));
    	}
    	actualBuffer++;
    	buffers.put(actualBuffer, newframes);
    	this.frames = newframes;
    }

    public void commit() {
    	
    }

    public DMD(int w, int h) {
        this.width = w;
        this.height = h;
        bytesPerRow = width / 8;
        if (width % 8 > 0)
            bytesPerRow++;
        frameSizeInByte = bytesPerRow * height;
        setNumberOfSubframes(2);
        buffers.put(actualBuffer, frames);
    }
    
    public void setNumberOfSubframes(int n) {
        frames.clear();
        for(int i = 0; i < n; i++) {
            frames.add( new byte[frameSizeInByte]);
        }
        frame1 = frames.get(0);
        frame2 = frames.get(1);
        numberOfSubframes = n;
    }

    public DMD() {
        this(128, 32);
    }

    public void copyInto(DMD src, int xsrc, int ysrc, int w, int h, int destx, int desty) {
    }

    public void setPixel(int x, int y, int v) {
    	int numberOfPlanes = frames.size();
    	if( v >= (1<<numberOfPlanes)) {
    		// extend
    		if( numberOfPlanes == 2) {
    			frames.add(new byte[frameSizeInByte]);
    			frames.add(Arrays.copyOf(frames.get(1), frameSizeInByte)); 	// must be a copy of plane 1		
    		}
    	}
    	byte mask = (byte) (128 >> (x % 8));
    	for(int plane = numberOfPlanes-1; plane>=0; plane--) {
    		if( (v & 0x01) != 0) {
    			frames.get(plane)[y*bytesPerRow+x/8] |= mask;
    		} else {
    			frames.get(plane)[y*bytesPerRow+x/8] &= ~mask;
    		}
    		v >>= 1;
    	}
    }

    public int getPixel(int x, int y) {
    	byte mask = (byte) (128 >> (x % 8));
    	int v = 0;
    	for(int plane = 0; plane <frames.size(); plane++) {
    		v += (frames.get(plane)[x / 8 + y * bytesPerRow] & mask) != 0 ? (1<<plane) : 0;
    	}
    	return v;
    }
    
//    int pitch = 7;
//    int offset = 20;
//
//    public Point transformCoord( int x, int y) {
//        return new Point((x-offset)/pitch,(y-offset)/pitch);   
//    }
//    
    public void setFrames(byte[] f1, byte[] f2) {
        this.frame1 = f1;
        this.frame2 = f2;
    }

    public void clear() {
        for (byte[] p : frames) {
			Arrays.fill(p, (byte)0);
		}
    }

    public void writeOr(Frame frame) {
        if (frame != null) {
        	if( frame.planes.size()!=3) {
        		while( frames.size()< frame.planes.size() ) {
        			frames.add(new byte[bytesPerRow*height]);
        		}
        		numberOfSubframes = frames.size();
        		for (int i = 0; i < frame.planes.size(); i++) {
					copyOr(frames.get(i),frame.planes.get(i).plane);
					
				}
        		//copyOr(frame1, frame.planes.get(0).plane);
        		//copyOr(frame2, frame.planes.get(1).plane);
        	}
        }
    }

    public void writeAnd(byte[] mask) {
        if (mask != null) {
            copyAnd(frame1, mask);
            copyAnd(frame2, mask);
        }
    }
    
    public void writeNotAnd(byte[] mask) {
        if (mask != null) {
            copyNotAnd(frame1, mask);
            copyNotAnd(frame2, mask);
        }
    }


    private void copyNotAnd(byte[] target, byte[] src) {
        for (int i = 0; i < src.length; i++) {
            target[i] = (byte) (target[i] & ~src[i]);
        }
    }

    private void copyAnd(byte[] target, byte[] src) {
        for (int i = 0; i < src.length; i++) {
            target[i] = (byte) (target[i] & src[i]);
        }
    }
    
    private void copyOr(byte[] target, byte[] src) {
        for (int i = 0; i < src.length; i++) {
            target[i] = (byte) (target[i] | src[i]);
        }
    }

    // masken zum setzen von pixeln
    int[] mask = { 
            0b01111111, 0b11011111, 0b11110111, 0b11111101,
            0b10111111, 0b11101111, 0b11111011, 0b11111110
    };

    public boolean getPixel(byte[] buffer, int x, int y) {
        int bitpos = 0;
        int yoffset = y * (width / 4);
        if (y >= height / 2) {
            bitpos = 4;
            yoffset = (y - height / 2) * (width / 4);
        }
        int index = yoffset + x / 4;
        return (buffer[index] & ~mask[(x & 3) + bitpos]) == 0;
    }

    public void setPixel(byte[] buffer, int x, int y, boolean on) {
        int bitpos = 0;
        int yoffset = y * (width / 4);
        if (y >= height / 2) {
            bitpos = 4;
            yoffset = (y - height / 2) * (width / 4);
        }
        int index = yoffset + x / 4;

        if (on) {
            buffer[index] &= mask[(x & 3) + bitpos];
        } else {
            buffer[index] |= ~mask[(x & 3) + bitpos];
        }
    }

    byte[] m2 = { (byte) 0b10000000, (byte) 0b01000000, (byte) 0b00100000, (byte) 0b00010000, (byte) 0b00001000,
            (byte) 0b00000100, (byte) 0b00000010, (byte) 0b00000001, };

    public byte[] transformFrame1(byte[] in) {
        byte[] t = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            t[i] = (byte) ~(in[i]);
        }
        return t;
    }

    public byte[] transformFrame(byte[] in) {
        byte[] t = new byte[in.length];
        // for(int i=0; i<t.length;i++) t[i] = (byte) 255;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean on = (in[y * bytesPerRow + x / 8] & m2[x % 8]) != 0;
                // System.out.print(on?"*":".");
                setPixel(t, x, y, on);
            }
            // System.out.println("");
        }
        return t;
    }

    public String dumpAsCode() {
        StringBuilder builder = new StringBuilder();
        builder.append(" { ");
        byte[] f = transformFrame(frame1);
        for (int i = 0; i < f.length; i++) {
            builder.append(String.format("0x%02X , ", f[i]));
        }
        builder.append(" }, \n");
        // builder.append("byte[] f2 = new byte { \n");
        // for(int i = 0; i<frame2.length;i++) {
        // builder.append(String.format("0x%02X , ", frame2[i]));
        // }
        // builder.append("}; \n");
        return builder.toString();
    }

    public void writeTo(DataOutputStream os) throws IOException {
        os.writeShort(width);
        os.writeShort(height);
        os.writeShort(frameSizeInByte);
        os.write(frame1);
        os.write(frame2);
    }

    public static DMD read(DataInputStream is) throws IOException {
        int w = is.readShort();
        int h = is.readShort();
        DMD dmd = new DMD(w, h);
        int sizeInByte = is.readShort();
        assert (sizeInByte == dmd.getFrameSizeInByte());
        is.read(dmd.frame1);
        is.read(dmd.frame2);
        return dmd;
    }

    public Frame getFrame() {
        return new Frame(width, height, frame1, frame2);
    }

    @Override
    public String toString() {
        return "DMD [width=" + width + ", height=" + height + "]";
    }

}
