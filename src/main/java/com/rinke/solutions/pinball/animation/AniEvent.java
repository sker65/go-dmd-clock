package com.rinke.solutions.pinball.animation;

import java.util.List;

import com.rinke.solutions.pinball.model.Frame;

public class AniEvent {
    public enum Type {
        ANI, CLOCK, CLEAR
    };

    public Type evtType;
    public int actFrame;
    public int delay;
    public int nPlanes;
    public int timecode;
    public Animation actAnimation;
    public List<byte[]> hashes;
    public Frame frame;

    public AniEvent(Type evtType ) {
        this( evtType,0,null,null,0,0,0,null);
    }
    
    public AniEvent(Type evtType, int actFrame, Animation actAnimation, List<byte[]> hashes, 
    		int tc, int delay, int nPlanes, Frame frame) {
        super();
        this.evtType = evtType;
        this.actFrame = actFrame;
        this.actAnimation = actAnimation;
        this.hashes = hashes;
        this.timecode = tc;
        this.delay = delay;
        this.nPlanes = nPlanes;
        this.frame = frame;
    }


}
