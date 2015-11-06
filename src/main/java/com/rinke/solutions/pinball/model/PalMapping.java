package com.rinke.solutions.pinball.model;

import java.util.Arrays;

public class PalMapping {
    public byte digest[];
    public Palette palette;
    public long durationInMillis;
    public int durationInFrames;
    
    public PalMapping(byte[] digest, Palette palette, long durationInMillis, int durationInFrames) {
        super();
        if( digest.length != 16 ) {
            throw new IllegalArgumentException("digest length must be 16");
        }
        this.digest = new byte[digest.length];
        System.arraycopy(digest, 0, this.digest, 0, 16);
        this.palette = palette;
        this.durationInMillis = durationInMillis;
        this.durationInFrames = durationInFrames;
    }

	@Override
	public String toString() {
		return "PalMapping [digest=" + Arrays.toString(digest) + ", palette="
				+ palette + ", durationInMillis=" + durationInMillis
				+ ", durationInFrames=" + durationInFrames + "]";
	}
    
    
    
}
