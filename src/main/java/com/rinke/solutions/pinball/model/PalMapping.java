package com.rinke.solutions.pinball.model;

import java.util.Arrays;

public class PalMapping {
    public byte digest[];
    public int palIndex;
    public long durationInMillis;
    public int durationInFrames;
    
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

	@Override
	public String toString() {
		return "PalMapping [digest=" + Arrays.toString(digest) + ", palIndex="
				+ palIndex + ", durationInMillis=" + durationInMillis
				+ ", durationInFrames=" + durationInFrames + "]";
	}
    
    
    
}
