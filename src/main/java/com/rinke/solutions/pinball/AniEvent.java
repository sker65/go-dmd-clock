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


}
