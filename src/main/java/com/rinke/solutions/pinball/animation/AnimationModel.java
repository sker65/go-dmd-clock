package com.rinke.solutions.pinball.animation;


public class AnimationModel extends AbstractModel{
	
	private int holdCycles;
	private int cycles;
	private int clockFrom;
	private boolean clockSmall = false;
	private int clockXOffset = 24;
	private int clockYOffset = 3;
	private boolean clockInFront = false;
	private Fsk fsk = Fsk.F18;
	private int transitionFrom;
	
	public enum Fsk { 
		F8(8), F12(12), F16(16), F18(18);
		public int n;

		private Fsk(int n) {
			this.n = n;
		} 
	};
	
	public void setFsk(int i) {
		for(Fsk f : Fsk.values()) {
			if( f.n == i) {
				firePropertyChange("fsk", this.fsk, this.fsk = f);
			}
		}
	}

	public int getFsk() {
		return fsk.n;
	}

	public int getHoldCycles() {
		return holdCycles;
	}

	public void setHoldCycles(int holdCycles) {
		firePropertyChange("this.holdCycles", this.holdCycles, this.holdCycles = holdCycles);
	}

	public int getCycles() {
		return cycles;
	}

	public void setCycles(int cycles) {
		firePropertyChange("this.cycles", this.cycles, this.cycles = cycles);
	}

	public int getClockFrom() {
		return clockFrom;
	}

	public void setClockFrom(int clockFrom) {
		firePropertyChange("this.clockFrom", this.clockFrom, this.clockFrom = clockFrom);
	}

	public boolean isClockSmall() {
		return clockSmall;
	}

	public void setClockSmall(boolean clockSmall) {
		firePropertyChange("this.clockSmall", this.clockSmall, this.clockSmall = clockSmall);
	}

	public int getClockXOffset() {
		return clockXOffset;
	}

	public void setClockXOffset(int clockXOffset) {
		firePropertyChange("this.clockXOffset", this.clockXOffset, this.clockXOffset = clockXOffset);
	}

	public int getClockYOffset() {
		return clockYOffset;
	}

	public void setClockYOffset(int clockYOffset) {
		firePropertyChange("this.clockYOffset", this.clockYOffset, this.clockYOffset = clockYOffset);
	}

	public boolean isClockInFront() {
		return clockInFront;
	}

	public void setClockInFront(boolean clockInFront) {
		firePropertyChange("this.clockInFront", this.clockInFront, this.clockInFront = clockInFront);
	}

	public int getTransitionFrom() {
		return transitionFrom;
	}

	public void setTransitionFrom(int transitionFrom) {
		firePropertyChange("this.transitionFrom", this.transitionFrom, this.transitionFrom = transitionFrom);
	}


}
