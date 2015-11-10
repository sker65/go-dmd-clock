package com.rinke.solutions.pinball;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Frame {
    
    public int width;
    public int height;
    public int delay;
    public long timecode;
    public List<Plane> planes = new ArrayList<>();

    public Frame(int delay,int w, int h) {
        this.delay = delay;
        width = w;
        height = h;
   }
    
    public Frame(int w, int h, byte[] plane1, byte[] plane2) {
        width = w;
        height = h;
        planes.add(new Plane((byte)0, plane1));
        planes.add(new Plane((byte)1, plane2));
    }
    
    
    @Override
    public String toString() {
        return "Frame [delay=" + delay + ", planes=" + planes + "]";
    }

    public void setPixel(int x, int y) {
        if( x>=0 && x< width && y>=0 && y<height) {
            int bytesPerRow = width / 8;
            //System.out.println("setPixel("+x+","+y+")");
            int offset = y*bytesPerRow + x / 8;
            planes.get(0).plane[offset] &= ~ (128>>x%8);
            planes.get(1).plane[offset] &= ~ (128>>x%8);
        }
    }
    
    List<byte[]> getHashes() {
        List<byte[]> res = new ArrayList<>();
        try {
            int j = 0;
            
            StringBuilder sb = new StringBuilder();
            for (Plane plane : planes) {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(transform(plane.plane), 0, plane.plane.length);
                res.add(md.digest());
            }
            return res;
        } catch (NoSuchAlgorithmException e) {
         
        }
        return res;
    }

    public static byte[] transform(byte[] plane) {
    	return transform(plane, 0, plane.length);
    }

	public static byte[] transform(byte[] data, int offset, int size) {
        byte[] res = new byte[size];
        for(int i = offset; i < size+offset; i++) {
            byte x = data[i];
            byte b = 0;
            for( int bit=0; bit<8; bit++){
                b<<=1;
                b|=( x &1);
                x>>=1;
            }
            res[i-offset]=b;
        }
        return res;
	}
}