package com.rinke.solutions.pinball.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Pair;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameLink;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.renderer.AnimatedGIFRenderer;
import com.rinke.solutions.pinball.renderer.DMDFRenderer;
import com.rinke.solutions.pinball.renderer.DummyRenderer;
import com.rinke.solutions.pinball.renderer.ImageIORenderer;
import com.rinke.solutions.pinball.renderer.PcapRenderer;
import com.rinke.solutions.pinball.renderer.PinDumpRenderer;
import com.rinke.solutions.pinball.renderer.PngRenderer;
import com.rinke.solutions.pinball.renderer.Renderer;
import com.rinke.solutions.pinball.renderer.RgbRenderer;
import com.rinke.solutions.pinball.renderer.VPinMameRawRenderer;
import com.rinke.solutions.pinball.renderer.VPinMameRenderer;
import com.rinke.solutions.pinball.renderer.VideoCapRenderer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Animation {
    
	protected String basePath = "./";
	protected String transitionsPath = "./";
	
	// teil der zum einlesen gebraucht wird
	public int start = 0;
	public int end = 0;
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
	
	private boolean mutable = false;
	private boolean dirty = false;
	private boolean projectAnimation = false; // true if this animation is only part of the project workspace (no separate file)
	private int palIndex = 0;
	private RGB[] aniColors;
	
	private boolean trueColor;
	protected boolean clockWasAdded;
	public int width;
	public int height;

	public enum EditMode {		
		// because ordinals used directly in ani format, one must not change order
		REPLACE("Replace",						false, false, false, false,  false, false, false, false), 
		COLMASK("ColorMask",					true,  false, false, false, false, false, false, false), 
		FIXED("Fixed", 							false, true,  true, true, false, false, false, false), 
		COLMASK_FOLLOW("ColorMask Sequence", 	true,  true,  true, true, false, false, true,  false),
		LAYEREDCOL("Layered ColorMask", 		true,  true,  true, true, false, false, false, true),
		REPLACE_FOLLOW("Replace Sequence",		false, true,  true, true, false, false, true,  false),
		LAYEREDREPLACE("Layered ReplaceMask",	false, true,  true, true, true,  true,  true,  true),
		;

		// label to display
		public final String label;
		// uses masks in scene
		public final boolean enableColorMaskDrawing;// draw only on upper planes
		public final boolean enableMaskDrawing;     // draw on mask planes
		public final boolean enableDetectionMask;	// controls enable of D-Mask button
		public final boolean enableDetectionMaskSpinner;	// controls enable of D-Mask Spinner
		public final boolean enableLayerMask;		// controls enable of L-Mask button
		public final boolean haveLocalMask;			// Masks Data is fetch from actual frame of the scene
		public final boolean pullFrameDataFromAssociatedRecording;	// for FollowXX Modes, we need to pull frame data from associated recording
		public final boolean haveSceneDetectionMasks; // edit mode uses scene local detections masks

		private EditMode(String label, boolean colMaskDraw, 
				boolean enableMaskDrawing, boolean enableDetectionMask, boolean enableDetectionMaskSpinner ,boolean layerMask, boolean haveLocalMask,
				boolean pullFrameDataFromAssociatedRecording, boolean haveSceneDetectionMasks) {
			this.label = label;
			this.enableMaskDrawing = enableMaskDrawing;
			this.enableColorMaskDrawing = colMaskDraw;
			this.enableDetectionMask = enableDetectionMask;
			this.enableDetectionMaskSpinner = enableDetectionMaskSpinner;
			this.enableLayerMask = layerMask;
			this.haveLocalMask = haveLocalMask;
			this.pullFrameDataFromAssociatedRecording = pullFrameDataFromAssociatedRecording;
			this.haveSceneDetectionMasks = haveSceneDetectionMasks;
		}

		public static EditMode fromOrdinal(byte emo) {
			for (EditMode em : values()) {
				if( em.ordinal() == emo ) return em;
			}
			return null;
		}
	}
	
	public List<Mask> getMasks() {
		return null;
	}
	
	private EditMode editMode = EditMode.FIXED;

	public void setAniColors(RGB[] rgb) {
		this.aniColors = rgb;
	}

	public RGB[] getAniColors() {
		return aniColors;
	}
	
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
	public int actFrame;
	boolean ended = false;
	private int actCycle;
	int holdCount = 0;
	private String desc;

	public CompiledAnimation cutScene( int start, int end, int actualNumberOfPlanes) {
		// create a copy of the animation
		DMD tmp = new DMD(width,height);
		CompiledAnimation dest = new CompiledAnimation(
				AnimationType.COMPILED, this.getName(),
				0, end-start, this.skip, 1, 0);
		dest.setMutable(true);
		dest.width = this.width;
		dest.height = this.height;
		//dest.setDirty(true);
		dest.setClockFrom(Short.MAX_VALUE);
		// rerender and thereby copy all frames
		CompiledAnimation cani = null;
		if( this instanceof CompiledAnimation ) {
			cani = (CompiledAnimation)this;
		}
		this.actFrame = start;
        int tcOffset = 0;
		for (int i = start; i <= end; i++) {
			Frame frame = this.render(tmp, false);
            log.debug("source frame {}",frame);
			Frame targetFrame = new Frame(frame);
			if( i == start ) tcOffset = frame.timecode;
            targetFrame.timecode -= tcOffset;
            targetFrame.frameLink = null;
            if (cani != null) {
            	if (cani.frames.get(i).frameLink != null)
            		targetFrame.frameLink = new FrameLink(cani.frames.get(i).frameLink.recordingName,cani.frames.get(i).frameLink.frame);
            	System.arraycopy(cani.frames.get(i).crc32, 0, targetFrame.crc32, 0, 4);
            }
            int marker = targetFrame.planes.size();
            byte[] emptyPlane = new byte[frame.getPlane(0).length];
			while( targetFrame.planes.size() < actualNumberOfPlanes ) {
				targetFrame.planes.add(new Plane((byte)marker++, emptyPlane));
			}
			log.debug("target frame {}",targetFrame);
			dest.frames.add(targetFrame);
		}
		log.debug("copied {} frames",dest.frames.size());
		log.debug("target ani {}",dest);
		return dest;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public int getFrameCount(DMD dmd) {
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
		if( last != null && last.delay > 0 ) return last.delay;
		return refreshDelay;
	}

	public void setRefreshDelay(int refreshDelay) {
		this.refreshDelay = refreshDelay;
	}
	public Animation(AnimationType type, String name, int start, int end, int skip,
			int cycles, int holdCycles) {
		this(type, name, start, end, skip,cycles, holdCycles,128, 32);
	}
	
	public Animation(AnimationType type, String name, int start, int end, int skip,
			int cycles, int holdCycles, int w, int h) {
		this.start = start;
		this.actFrame = start;
		this.end = end;
		this.skip = skip;
		this.cycles = cycles;
		actCycle = 0;
		this.name = name;
		this.holdCycles = holdCycles;
		this.type = type;
		this.clockFrom = Integer.MAX_VALUE;
		this.editMode = EditMode.FIXED;
		this.width = w;
		this.height = h;
	}

	Renderer renderer = null;
	Properties props = new Properties();
	Frame last;
	Renderer transitionRenderer = null;
	List<Frame> transitions = new ArrayList<>();
	protected ProgressEventListener progressEventListener;
	
	protected Frame renderFrame(String name, DMD dmd, int act) {	
		Frame f = renderer.convert(name, dmd, act);
		int noFrames = renderer.getFrames().size();
		if( start == 0 && end > noFrames ) {
			end = noFrames-1;
		}
		return f;
	}
	
	public Renderer getRenderer() {
		if( renderer == null ) init(this.progressEventListener);
		return renderer;
	}
	
	public long getTimeCode(int actFrame) {
	    return renderer.getTimeCode(actFrame);
	}
	
	public void init(DMD dmd) {
		if( renderer != null ) {
			return;
		}
		if( transitionName != null && transitions.isEmpty() ) {
			initTransition(dmd);
		}

		if( renderer == null ) init(this.progressEventListener);
		setDimension(dmd.getWidth(),dmd.getHeight());
		// just to trigger image read
		renderer.getMaxFrame(basePath+name, dmd);
		if( renderer.getPalette() != null ) {
			aniColors = renderer.getPalette().colors;
		}
		Properties rendererProps = renderer.getProps();
		String width = rendererProps.getProperty("width");
		int planeSize = renderer.getFrames().get(0).getPlane(0).length;
		if( width!= null && Integer.parseInt(width) != dmd.getWidth()) {
			int w = Integer.parseInt(rendererProps.getProperty("width"));
			int h = Integer.parseInt(rendererProps.getProperty("height"));
			if(w*h/8 == planeSize) {
				setDimension(w, h);
			}
		}
	}
	
	// hook to override if needed by some animations
	protected void postInit(Renderer renderer) {}

	public void setDimension(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public Frame render(DMD dmd, boolean stop) {
		init(dmd);
		
		Frame frame = null;
		boolean doPostInit = renderer.getFrames().isEmpty();
		int maxFrame = renderer.getMaxFrame(basePath+name, dmd);
		if( doPostInit ) postInit(renderer);
		
		if(  maxFrame > 0 && end <= 0) end = maxFrame-1;
		if (actFrame <= end) {
			ended = false;
			last = renderFrame(basePath+name, dmd, actFrame);
			if( !stop && actFrame < end ) actFrame += skip;
		} else if (++actCycle < cycles) {
			actFrame = start;
		} else {
			if (holdCount++ >= holdCycles && transitionCount>=transitions.size()) {
			    ended = true;
			}
			actCycle = 0;
		}
		if( actFrame == end ) ended = true;
		frame = last;
		if( transitionFrom != 0 // it has a transition
		    && actFrame > transitionFrom  // it has started
			&& transitionCount<=transitions.size() // and not yet ended
				) {
		    frame = addTransitionFrame(frame);
			transitionCount++;
		}
		return frame;
	}
	
	protected Frame addTransitionFrame(Frame in) {
        Frame tframe = transitions.get(transitionCount < transitions.size() ? transitionCount : transitions.size() - 1);
        Frame r = new Frame(in);
        r.setMask(tframe.mask);
        return r;
    }

    public void initTransition(DMD dmd) {
	    log.debug("init transition: "+transitionName);
	    transitions.clear();
	    transitionCount=1;
		while(true) {
			Frame frame;
			try {
				frame = transitionRenderer.convert(transitionsPath+"transitions", dmd, transitionCount++);
				frame.planes.forEach( p -> p.marker = 0x6a);
				transitions.add(frame);
			} catch( RuntimeException e) {
			    log.info(e.getMessage());
				break;
			}
		}
		transitionCount=0;
	}

	public boolean addClock() {
		return  actFrame>clockFrom || (transitionFrom>0 && actFrame>=transitionFrom) ;
	}

	protected void init(ProgressEventListener listener) {
		switch (type) {
		case PNG:
			renderer = new PngRenderer(pattern,autoMerge);
			break;
		case DMDF:
			renderer = new DMDFRenderer();
			break;
		case GIF:
			renderer = new AnimatedGIFRenderer();
			break;
		case MAME:
			renderer = new VPinMameRenderer();
			break;
		case PCAP:
			renderer = new PcapRenderer();
			break;
		case COMPILED:
			renderer = new DummyRenderer();
			break;
		case PINDUMP:
			renderer = new PinDumpRenderer();
			break;
		case VIDEO:
			renderer = new VideoCapRenderer();
			break;
		case IMGIO:
			renderer = new ImageIORenderer(pattern);
			break;
		case RGB:
			renderer = new RgbRenderer();
			break;
		case RAW:
			renderer = new VPinMameRawRenderer();
			break;
		default:
			break;
		}
		renderer.setProps(props);
		renderer.setProgressEvt(listener);
	}

	public boolean hasEnded() {
		return ended;
	}

	public void restart() {
		ended = false;
		actCycle = 0;
		actFrame = start;
		holdCount = 0;
		transitionCount=0;
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
		if (actFrame < end) {
			actFrame += skip;
		}
	}

	public void prev() {
		if (actFrame > start) {
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
		transitions.clear();
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

    public String getTransitionsPath() {
        return transitionsPath;
    }

    public void setTransitionsPath(String transitionsPath) {
        this.transitionsPath = transitionsPath;
    }
    
    public Pair<String,String> getIconAndText() {
    	if( !isMutable() ) return Pair.of("fixed", desc);
    	return Pair.of(editMode.name().toLowerCase(), desc);
    }

    @Override
    public String toString() {
        return "Animation [width="+width+", height="+height+", start=" + start + ", end=" + end + ", skip=" + skip + ", cycles=" + cycles + ", name=" + name
                + ", holdCycles=" + holdCycles + ", type=" + type + ", refreshDelay=" + refreshDelay + ", clockFrom="
                + clockFrom + ", clockSmall=" + clockSmall + ", clockXOffset=" + clockXOffset + ", clockYOffset="
                + clockYOffset + ", clockInFront=" + clockInFront + ", fsk=" + fsk + ", transitionFrom=" + transitionFrom
                + ", transitionName=" + transitionName + ", transitionDelay=" + transitionDelay + ", desc=" + desc + "]";
    }

	public void commitDMDchanges(DMD dmd) {
	}

	public boolean isMutable() {
		return mutable;
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public int getPalIndex() {
		return palIndex;
	}

	public void setPalIndex(int palIndex) {
		this.palIndex = palIndex;
	}

	public Properties getProps() {
		return props;
	}

	public void setProps(Properties props) {
		 this.props = props;
	}

	public boolean isTrueColor() {
		return trueColor;
	}

	public void setTrueColor(boolean trueColor) {
		 this.trueColor = trueColor;
	}

	public EditMode getEditMode() {
		return editMode;
	}

	public void setEditMode(EditMode editMode) {
		this.editMode = editMode;
	}

	public boolean isClockWasAdded() {
		return clockWasAdded;
	}

	public void setClockWasAdded(boolean clockWasAdded) {
		this.clockWasAdded = clockWasAdded;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		 this.dirty = dirty;
	}

	public boolean isProjectAnimation() {
		return projectAnimation;
	}

	public void setProjectAnimation(boolean projectAnimation) {
		this.projectAnimation = projectAnimation;
	}

	public void setProgressEventListener(ProgressEventListener listener) {
		this.progressEventListener = listener;
		
	}

	// just a workaround, todo remove actFrame completely from Animation?
	public Frame render(int frameNo, DMD dmd, boolean stop) {
		int tmp = actFrame;
		actFrame = frameNo;
		Frame r = render(dmd, stop);
		actFrame = tmp;
		return r;
	}
	
}
