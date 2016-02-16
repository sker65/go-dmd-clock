package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import com.rinke.solutions.pinball.model.Model;

public class Frame implements Model {
    
    public int delay;
    public int timecode;
    public List<Plane> planes = new ArrayList<>();

    public Frame(Frame src) {
    	this.delay = src.delay;
    	this.timecode = src.timecode;
    	for (Plane p : src.planes) {
			planes.add(new Plane(p.marker, p.plane));
		}
	}

	public Frame(byte[] plane1, byte[] plane2) {
        planes.add(new Plane((byte)0, plane1));
        planes.add(new Plane((byte)1, plane2));
    }

    public Frame( byte[] plane1, byte[] plane2, byte[] plane3, byte[] plane4) {
        planes.add(new Plane((byte)0, plane1));
        planes.add(new Plane((byte)1, plane2));
        planes.add(new Plane((byte)2, plane3));
        planes.add(new Plane((byte)3, plane4));
    }

    public Frame() {
	}

	public List<byte[]> getHashes() {
        List<byte[]> res = new ArrayList<>();
//        try {
            for (Plane plane : planes) {
                CRC32 crc = new CRC32();
                crc.update(transform(plane.plane), 0, plane.plane.length);
                byte[] b = new byte[4];
                long l = crc.getValue();
                for( int i = 3; i>=0; i--) {
                    b[i] = (byte) (l & 0xFF);
                    l >>= 8;
                }
                res.add(b);
//                MessageDigest md = MessageDigest.getInstance("MD5");
//                md.update(transform(plane.plane), 0, plane.plane.length);
//                res.add(md.digest());
            }
            return res;
//        } catch (NoSuchAlgorithmException e) {
//         
//        }
//        return res;
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

    @Override
    public String toString() {
        return "Frame [delay=" + delay + ", timecode=" + timecode + ", planes="
                + planes + "]";
    }

	@Override
	public void writeTo(DataOutputStream os) throws IOException {
		os.writeInt(delay);
		os.writeShort(planes.size());
		if( !planes.isEmpty() ) {
            os.writeShort(planes.get(0).plane.length);
		}
		for(Plane p : planes) {
			os.write(p.plane);
		}
	}

}