package com.rinke.solutions.pinball.animation;

import java.util.List;

public class AniEvent {
    public enum Type {
        ANI, CLOCK, CLEAR
    };

    public Type evtType;
    public int actFrame;
    public long timecode;
    public Animation actAnimation;
    public List<byte[]> hashes;

    public AniEvent(Type evtType, int actFrame, Animation actAnimation, List<byte[]> hashes, long tc) {
        super();
        this.evtType = evtType;
        this.actFrame = actFrame;
        this.actAnimation = actAnimation;
        this.hashes = hashes;
        this.timecode = tc;
    }


}
