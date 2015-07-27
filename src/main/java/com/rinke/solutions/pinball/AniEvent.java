package com.rinke.solutions.pinball;

public class AniEvent {
	enum Type { ANI,CLOCK };
	Type evtType;
	int actFrame;
	Animation actAnimation;
	public AniEvent(Type evtType, int actFrame, Animation actAnimation) {
		super();
		this.evtType = evtType;
		this.actFrame = actFrame;
		this.actAnimation = actAnimation;
	}

	
}
