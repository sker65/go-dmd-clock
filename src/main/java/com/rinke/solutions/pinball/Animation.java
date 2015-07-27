package com.rinke.solutions.pinball;

import java.util.ArrayList;
import java.util.List;

import com.rinke.solutions.pinball.renderer.AnimatedGIFRenderer;
import com.rinke.solutions.pinball.renderer.DMDFRenderer;
import com.rinke.solutions.pinball.renderer.FrameSet;
import com.rinke.solutions.pinball.renderer.PngRenderer;
import com.rinke.solutions.pinball.renderer.Renderer;
import com.rinke.solutions.pinball.renderer.VPinMameRenderer;


public class Animation {

	protected String basePath = "./";
	
	// teil der zum einlesen gebraucht wird
	protected int start = 0;
	protected int end = 0;
	public  int skip = 2;
	private String pattern = "Image-0x%04X";
	private boolean autoMerge;

	// meta daten
	private int cycles = 1;
	private String name;
	private int holdCycles;
	private AnimationType type;
	private int refreshDelay = 100;
	// defines at which frame clock should reappear
	private int clockFrom;
	// should we use small clock in animation
	private boolean clockSmall = false;
	private int clockXOffset = 24;
	private int clockYOffset = 3;
	private boolean clockInFront = false;
	private int fsk = 16;
	
	private int transitionFrom = 0;
	private String transitionName = null;
	private int transitionCount = 1;
	private int transitionDelay = 50;
	
	public int getFsk() {
		return fsk;
	}

	public void setFsk(int fsk) {
		this.fsk = fsk;
	}

	public boolean isClockInFront() {
		return clockInFront;
	}

	public void setClockInFront(boolean clockInFront) {
		this.clockInFront = clockInFront;
	}

	// runtime daten
	int actFrame;
	boolean ended = false;
	private int actCycle;
	int holdCount = 0;
	
	private String desc;

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public int getFrameSetCount(DMD dmd) {
		int r =  ((end-start)/skip)+1;
		// make use of transition length
		if( transitionFrom>0) {
			initTransition(dmd);
			r += transitions.size()-1;
			r -= (end-transitionFrom)/skip;
		}
		return r;
	}
	
	public int getCycles() {
		return cycles;
	}

	public void setCycles(int cycles) {
		this.cycles = cycles;
	}

	public int getHoldCycles() {
		return holdCycles;
	}

	public void setHoldCycles(int holdCycles) {
		this.holdCycles = holdCycles;
	}

	public AnimationType getType() {
		return type;
	}

	public void setType(AnimationType type) {
		this.type = type;
	}

	public int getClockFrom() {
		return clockFrom;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setClockFrom(int clockFrom) {
		this.clockFrom = clockFrom;
	}

	public String getName() {
		return name;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public int getRefreshDelay() {
		if( last != null && last.duration > 0 ) return last.duration;
		return refreshDelay;
	}

	public void setRefreshDelay(int refreshDelay) {
		this.refreshDelay = refreshDelay;
	}

	public Animation(AnimationType type, String name, int start, int end, int skip,
			int cycles, int holdCycles) {
		super();
		this.start = start;
		this.actFrame = start;
		this.end = end;
		this.skip = skip;
		this.cycles = cycles;
		actCycle = 0;
		this.name = name;
		this.holdCycles = holdCycles;
		this.type = type;
		this.clockFrom = 20000;
	}

	Renderer r = null;
	FrameSet last;
	Renderer transitionRenderer = null;
	List<FrameSet> transitions = new ArrayList<>();
	
	protected FrameSet renderFrameSet(String name, DMD dmd, int act) {
		return r.convert(name, dmd, act);
	}
	
	public Renderer getRenderer() {
		if( r == null ) init();
		return r;
	}
	
	public List<FrameSet> render(DMD dmd, boolean stop) {
		
		if( transitionName != null && transitions.isEmpty() ) {
			initTransition(dmd);
		}
		
		List<FrameSet> res = new ArrayList<>();
		if( r == null ) init();
		if (actFrame <= end) {
			ended = false;
			last = renderFrameSet(basePath+name, dmd, actFrame);
			if( !stop) actFrame += skip;
			if( r.getMaxFrame() > 0 && end == 0) end = r.getMaxFrame()-1;
		} else if (++actCycle < cycles) {
			actFrame = start;
		} else {
			if (holdCount++ >= holdCycles && transitionCount>=transitions.size())
				ended = true;
			actCycle = 0;
		}
		res.add( last );
		if( transitionFrom != 0 && actFrame > transitionFrom &&
				transitionCount<transitions.size()) {
			res.add(transitions.get(transitionCount++));
		}
		return res;
	}
	
	private void initTransition(DMD dmd) {
		while(true) {
			FrameSet f;
			try {
				f = transitionRenderer.convert(basePath+"transitions", dmd, transitionCount++);
				transitions.add(f);
			} catch( RuntimeException e) {
				break;
			}
		}
		transitionCount=0;
	}

	public boolean addClock() {
		return actFrame>clockFrom;
	}

	private void init() {
		switch (type) {
		case PNG:
			r = new PngRenderer(pattern,autoMerge);
			break;
		case DMDF:
			r = new DMDFRenderer();
			break;
		case GIF:
			r = new AnimatedGIFRenderer();
			break;
		case MAME:
			r = new VPinMameRenderer();
			break;
		default:
			break;
		}
	}

	public boolean hasEnded() {
		return ended;
	}

	public void restart() {
		ended = false;
		actCycle = 0;
		actFrame = start;
		holdCount = 0;
	}

	public void setAutoMerge(boolean b) {
		this.autoMerge = b;
	}

	public void setClockSmall(boolean clockSmall) {
		this.clockSmall = clockSmall;
	}

	public void setClockXOffset(int clockXOffset) {
		this.clockXOffset = clockXOffset;
	}

	public void setClockYOffset(int clockYOffset) {
		this.clockYOffset = clockYOffset;
	}

	public boolean isClockSmall() {
		return clockSmall;
	}

	public int getClockXOffset() {
		return clockXOffset;
	}

	public int getClockYOffset() {
		return clockYOffset;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void next() {
		if (actFrame <= end) {
			actFrame += skip;
		}
	}

	public void prev() {
		if (actFrame >= start) {
			actFrame -= skip;
		}
	}

	public void setPos(int pos) {
		if( pos >= start && pos <= end ) {
			actFrame = pos;
		}
	}

	public int getTransitionFrom() {
		return transitionFrom;
	}

	public void setTransitionFrom(int transitionFrom) {
		this.transitionFrom = transitionFrom;
	}

	public String getTransitionName() {
		return transitionName;
	}

	public void setTransitionName(String transitionName) {
		this.transitionName = transitionName;
		this.transitionRenderer = new PngRenderer(transitionName+"%d",false);
	}

	public int getTransitionDelay() {
		return transitionDelay;
	}

	public void setTransitionDelay(int transitionDelay) {
		this.transitionDelay = transitionDelay;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public int getActFrame() {
		return actFrame;
	}

	public void setActFrame(int actFrame) {
		this.actFrame = actFrame;
	}

}
