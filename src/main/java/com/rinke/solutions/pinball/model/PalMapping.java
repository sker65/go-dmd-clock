package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class PalMapping implements Model {
    public byte digest[];
    public int palIndex;
    public long durationInMillis;
    public int durationInFrames;
    public int hashIndex; // which hash (from which frame)
    public String name;
    public int animationIndex;
    public int frameIndex;
    public String frameSeqName;
    
    public PalMapping(byte[] digest, int palIndex, long durationInMillis, int durationInFrames) {
        super();
        if( digest.length != 16 ) {
            throw new IllegalArgumentException("digest length must be 16");
        }
        this.digest = new byte[digest.length];
        System.arraycopy(digest, 0, this.digest, 0, 16);
        this.palIndex = palIndex;
        this.durationInMillis = durationInMillis;
        this.durationInFrames = durationInFrames;
    }

	public PalMapping(int palIndex, String name) {
		this.palIndex = palIndex;
		this.name = name;
	}

	public void writeTo(DataOutputStream os) throws IOException {
		os.write(digest);
		os.writeShort(palIndex);
		os.writeLong(durationInMillis);
		os.writeShort(durationInFrames);
	}

    public void setDigest(byte[] digest) {
        this.digest = new byte[16];
        System.arraycopy(digest, 0, this.digest, 0, 16);
    }

	@Override
	public String toString() {
		return "PalMapping [digest=" + Arrays.toString(digest) + ", palIndex="
				+ palIndex + ", durationInMillis=" + durationInMillis
				+ ", durationInFrames=" + durationInFrames + ", hashIndex="
				+ hashIndex + ", name=" + name + "]";
	}

    
    
}
