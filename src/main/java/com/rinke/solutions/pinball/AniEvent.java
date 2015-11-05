package com.rinke.solutions.pinball;

public class AniEvent {
	enum Type { ANI,CLOCK };
	Type evtType;
	int actFrame;
	long timecode;
	Animation actAnimation;
	String hashes;
	public AniEvent(Type evtType, int actFrame, Animation actAnimation, String hashes, long tc) {
		super();
		this.evtType = evtType;
		this.actFrame = actFrame;
		this.actAnimation = actAnimation;
		this.hashes  = hashes;
		this.timecode = tc;
	}

	
}
