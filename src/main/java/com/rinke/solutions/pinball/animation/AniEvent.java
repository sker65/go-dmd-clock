package com.rinke.solutions.pinball.animation;

import com.rinke.solutions.pinball.model.Frame;

public class AniEvent {
    public enum Type {
        ANI, CLOCK, CLEAR, FRAMECHANGE
    };

    public final Type evtType;
    public final Animation ani;
    public final Frame frame;

    public AniEvent(Type evtType ) {
        this(evtType,null,null);
    }
    
    public AniEvent(Type evtType, Animation actAnimation, Frame frame) {
        super();
        this.evtType = evtType;
        this.ani = actAnimation;
        this.frame = frame;
    }


}
