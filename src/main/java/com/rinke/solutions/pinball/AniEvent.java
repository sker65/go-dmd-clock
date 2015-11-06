package com.rinke.solutions.pinball;

import java.util.List;

public class AniEvent {
    enum Type {
        ANI, CLOCK
    };

    Type evtType;
    int actFrame;
    long timecode;
    Animation actAnimation;
    List<byte[]> hashes;

    public AniEvent(Type evtType, int actFrame, Animation actAnimation, List<byte[]> hashes, long tc) {
        super();
        this.evtType = evtType;
        this.actFrame = actFrame;
        this.actAnimation = actAnimation;
        this.hashes = hashes;
        this.timecode = tc;
    }

    public String getPrintableHashes() {
        StringBuffer hexString = new StringBuffer();
        int plane = 0;
        for(byte[] p: hashes) {
            for( int j = 0; j<p.length; j++) 
                hexString.append(String.format("%02X ", p[j]));
            hexString.append("plane " + (plane++) + ": " + hexString.toString());
        }
        return hexString.toString();
    }

}
