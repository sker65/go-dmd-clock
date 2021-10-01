package com.rinke.solutions.pinball;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Plane;

@Slf4j
public class DMD extends Observable {

	// geometrie
	private int width;
	private int height;
	private int bytesPerRow;
	private int planeSize;
	
    // limits drawing (setPixel) to planes, that are not masked
    private int drawMask = Constants.DEFAULT_DRAW_MASK;

    // optinal for debugging purpose: a histogram of color values
    Map<Integer,Integer> colHist = new HashMap<>();

    // the frame that holds image data
    private Frame frame = new Frame();
    // a map of frame for undo / redo
    public Map<Integer,Frame> buffers = new HashMap<>();
    // "pointer" into the map of frame buffers
    int actualBuffer = 0;
    
    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getPlaneSize() {
        return this.planeSize;
    }

    public int getBytesPerRow() {
        return this.bytesPerRow;
    }

    public void copyLastBuffer() {
    	if( actualBuffer>0) {
        	//Frame target = buffers.get(actualBuffer);
    		log.trace("copyLastBuffer buffer {} -> {}, max: {}", actualBuffer-1, actualBuffer, buffers.size());
        	frame = new Frame(buffers.get(actualBuffer-1));
        	buffers.put(actualBuffer, frame);
    	}
    }
    
    public void addUndoBuffer() {
    	log.trace("addUndoBuffer()");
    	Frame newframe = new Frame(frame);
    	actualBuffer++;
    	log.trace("add undo: {}, max: {}", actualBuffer, buffers.size());
    	buffers.put(actualBuffer, newframe);
    	updateActualBuffer(actualBuffer);
    }
    
    public void undo() {
    	if(canUndo()) {
    		actualBuffer--;
        	log.trace("undo: {}, max: {}", actualBuffer, buffers.size());
    		updateActualBuffer(actualBuffer);
    	}
    }

    public void redo() {
    	if( canRedo() ) {
    		actualBuffer++;
    		log.trace("redo: {}, max: {}", actualBuffer, buffers.size());
    		updateActualBuffer(actualBuffer);
    	}
    }
    
    public boolean canRedo() {
    	return buffers.size()-1>actualBuffer;
    }
    
    public boolean canUndo() {
    	return  actualBuffer>0;
    }
    
	public void setSize(int w, int h) {
		setSize(w, h, false);
	}

	public void setSize(int w, int h, boolean preserveMask) {
		this.width = w;
		this.height = h;
		bytesPerRow = width / 8;
		if (width % 8 > 0)
			bytesPerRow++;
		planeSize = bytesPerRow * height;
		if( preserveMask ) {
			Mask m = frame.mask;
			frame = new Frame();
			frame.setMask(m);
		} else {
			frame = new Frame();
		}
		setNumberOfPlanes(2);
		actualBuffer = 0;
		buffers.clear();
		buffers.put(actualBuffer, frame);
	}

    public DMD(int w, int h) {
    	setSize(w, h);
        setNumberOfPlanes(2);
        buffers.put(actualBuffer, frame);
        setChanged();
    }
    
    public DMD(DmdSize size) {
		this(size.width, size.height);
	}

	public void setNumberOfPlanes(int n) {
		if(frame.planes.size() != 0 && frame.planes.get(0).data.length != this.planeSize) {
			while( 0 < frame.planes.size() ) {
	            frame.planes.remove(frame.planes.size()-1);
	    	}
		}
		if( n == frame.planes.size() ) return;
		log.trace("dmd setNumberOfSubframes {}", n );
    	while( n < frame.planes.size() ) {
    		log.trace("removing plane {}", frame.planes.size()-1);
            frame.planes.remove(frame.planes.size()-1);
    	}
    	while( n > frame.planes.size() ) {
    		log.trace("adding plane {}", frame.planes.size());
    		frame.planes.add( new Plane((byte)frame.planes.size(),new byte[this.planeSize]));
    	}
    	log.trace("final plane no: {} ", frame.planes.size());
    }

    public void setPixel(int x, int y, int v) {
    	if( rangeCheck(x,y) ) return;
    	byte mask = (byte) (0b10000000 >> (x % 8));
        if( maskIsRelevant() ) {
    		if( (v & 0x01) != 0) {
    			frame.mask.data[y*this.bytesPerRow+x/8] |= mask;
    		} else {
    			frame.mask.data[y*this.bytesPerRow+x/8] &= ~mask;
    		}
        }
        int drawMask1 = drawMask >> 1;
    	int numberOfPlanes = frame.planes.size();
    	for(int plane = 0; plane < numberOfPlanes; plane++) {
    		if( ((1<<plane) & drawMask1) != 0) {
        		if( (v & 0x01) != 0) {
        			frame.planes.get(plane).data[y*this.bytesPerRow+x/8] |= mask;
        		} else {
        			frame.planes.get(plane).data[y*this.bytesPerRow+x/8] &= ~mask;
        		}
    		}
    		v >>= 1;
    	}
    }
    
    public int getPixelWithoutMask(int x, int y) {
    	if( rangeCheck(x,y) ) return 0;
    	byte mask = (byte) (0b10000000 >> (x % 8));
    	int v = 0;
    	for(int plane = 0; plane < frame.planes.size(); plane++) {
    		v += (frame.planes.get(plane).data[x / 8 + y * this.bytesPerRow] & mask) != 0 ? (1<<plane) : 0;
    	}
    	return v;
    }
    
    private boolean rangeCheck(int x, int y) {
		return ( x<0 || y <0 || x >= this.width || y >= this.height );
	}

	private boolean maskIsRelevant() {
    	return (drawMask & 1) != 0 && frame.hasMask();
    }
	
	public int getMaskPixel(int x, int y) {
    	if( rangeCheck(x,y) ) return 0;
    	byte mask = (byte) (0b10000000 >> (x % 8));
    	int v = 0;
    	v += (frame.mask.data[x / 8 + y * this.bytesPerRow] & mask) != 0 ? 1 : 0;
    	return v;
    }
   
	public void setMaskPixel(int x, int y, int v) {
		if( rangeCheck(x,y) ) return;
    	byte mask = (byte) (0b10000000 >> (x % 8));
		if( (v & 0x01) != 0) {
			frame.mask.data[y*this.bytesPerRow+x/8] |= mask;
		} else {
			frame.mask.data[y*this.bytesPerRow+x/8] &= ~mask;
		}
    }

	public int getPixel(int x, int y) {
    	if( rangeCheck(x,y) ) return 0;
    	byte mask = (byte) (0b10000000 >> (x % 8));
    	int v = 0;
    	if( maskIsRelevant() ) {
    		v += (frame.mask.data[x / 8 + y * this.bytesPerRow] & mask) != 0 ? 1 : 0;
    	}
    	int drawMask1 = drawMask >> 1;
    	for(int plane = 0; plane < frame.planes.size(); plane++) {
    		if( ((1<<plane) & drawMask1) != 0) {
    			v += (frame.planes.get(plane).data[x / 8 + y * this.bytesPerRow] & mask) != 0 ? (1<<plane) : 0;
    		}
    	}
    	return v;
    }
   
    public void clear() {
    	updateActualBuffer(0);
    	log.trace("DMD.clear()");
        for (Plane p : frame.planes) {
			Arrays.fill(p.data, (byte)0);
		}
    }

    public void writeOr(Frame src) {
        if (src != null) {
        	//if( src.planes.size()!=3) {
        		if( frame.planes.size() < src.planes.size() ) {
        			frame.planes.clear();
        		}
        		int i = 0;
        		while( frame.planes.size() < src.planes.size() ) {
        			frame.planes.add(new Plane(src.planes.get(i++).marker,new byte[this.planeSize]));
        		}
        		for ( i = 0; i < src.planes.size(); i++) {
					copyOr(frame.planes.get(i).data,src.planes.get(i).data);
				}
        	//s}
        }
    }

    public void writeAnd(byte[] mask) {
        if (mask != null) {
        	for(Plane p:frame.planes) {
        		copyAnd(p.data, mask);
        	}
        }
    }
    
    public void writeNotAnd(byte[] mask) {
        if (mask != null) {
        	for(Plane p:frame.planes) {
        		copyNotAnd(p.data, mask);
        	}
        }
    }

    private void copyNotAnd(byte[] target, byte[] src) {
        for (int i = 0; i < src.length; i++) {
            target[i] = (byte) (target[i] & ~src[i]);
        }
    }

    private void copyAnd(byte[] target, byte[] src) {
        for (int i = 0; i < src.length; i++) {
            target[i] = (byte) (target[i] & src[i]);
        }
    }
    
    private void copyOr(byte[] target, byte[] src) {
        for (int i = 0; i < src.length; i++) {
            target[i] = (byte) (target[i] | src[i]);
        }
    }
    
    // wird nur zum rendern der Zeit verwendet -> TODO refactor
    public void copy(int yoffset, int xoffset, DMD src, boolean low, boolean mask) {
    	log.trace("DMD.copy()");
    	byte[] frame1 = frame.getPlane(0);
    	byte[] frame2 = frame.getPlane(1);
    	
        for (int row = 0; row < src.getHeight(); row++) {
            for (int col = 0; col < src.getWidth() / 8; col++) {
                if(mask) 
                    frame1[(row + yoffset) * getBytesPerRow() + xoffset + col] &= 
                        ~src.frame.getPlane(0)[src.getBytesPerRow() * row + col];
                else
                    frame1[(row + yoffset) * getBytesPerRow() + xoffset + col] = 
                        src.frame.getPlane(0)[src.getBytesPerRow() * row + col];
                
                if (!low) {
                    if( mask )
                        frame2[(row + yoffset) * getBytesPerRow() + xoffset + col] &= 
                            ~src.frame.getPlane(0)[src.getBytesPerRow() * row + col];
                    else
                        frame2[(row + yoffset) * getBytesPerRow() + xoffset + col] = 
                            src.frame.getPlane(0)[src.getBytesPerRow() * row + col];

                }
            }
        }
    }
	

    // masken zum setzen von pixeln
    private int[] pixmask = { 
            0b01111111, 0b11011111, 0b11110111, 0b11111101,
            0b10111111, 0b11101111, 0b11111011, 0b11111110
    };

    public Frame getFrame() {
    	log.trace("getFrame()");
        return frame;
    }

    public void setFrame(Frame frame) {
    	log.trace("setFrame(Frame@{})", Integer.toHexString(frame.hashCode()));
		this.frame = frame;
		buffers.put(actualBuffer, frame);
		setChanged();
    	notifyObservers();
	}

	public void updateActualBuffer(int i) {
		this.actualBuffer = i;
		frame = buffers.get(actualBuffer);
		log.trace("actual buffer is: {}, {}", actualBuffer, frame);
    	setChanged();
    	notifyObservers();
	}

	public void dumpHistogram() {
		colHist.clear();
		for (int row = 0; row < this.height; row++) {
			for (int col = 0; col < this.width; col++) {
				byte mask = (byte) (0b10000000 >> (col % 8));
				int v = 0;
				for (int i = 0; i < frame.planes.size(); i++) {
					// if( col / 8 + row * bytesPerRow <
					// frame.getPlane(i).length) {
					v += (frame.getPlane(i)[col / 8 + row * this.bytesPerRow] & mask) != 0 ? (1 << i) : 0;
					// }
				}
				stat(v);
			}
		}
		log.trace("DMD@{} with Frame@{} hist: {}, {}", 
				Integer.toHexString(hashCode()), 
				Integer.toHexString(frame.hashCode()), 
				this.dumpHist(), this.toString());
	}
	
	private String dumpHist() {
		StringBuilder sb = new StringBuilder();
		for(int v: colHist.keySet()) {
			sb.append("v: "+v+ " -> "+colHist.get(v)+" ");
		}
		return sb.toString();
	}

	private void stat(int v) {
		Integer count = colHist.get(v);
		if( count == null ) {
			colHist.put(v, new Integer(1));
		} else {
			colHist.put(v, new Integer(count+1));
		}
	}

    public int getNumberOfPlanes() {
        return frame.planes.size();
    }

	public int getDrawMask() {
		return drawMask;
	}

	/**
	 * binary mask for drawing planes. bit 0 -> mask plane
	 * bit 1 -> first color plane ....
	 * @param drawMask
	 */
	public void setDrawMask(int drawMask) {
		 this.drawMask = drawMask;
		 log.trace("setting draw mask to {}", Integer.toBinaryString(drawMask));
	}

	public boolean hasMask() {
		return frame.hasMask();
	}

	public void invertMask() {
		if( hasMask() ) { // TODO check why this is called sometimes without mask
			addUndoBuffer();
			byte[] data = frame.mask.data;
			for( int i = 0; i < data.length; i++) {
				data[i] = (byte) ~data[i];
			}
			setMask(new Mask(data, frame.mask.locked));
		}
	}

	public void setMask(Mask mask) {
		frame.setMask(mask);
	}

	public void fill(byte val) {
		log.trace("fill({})", val);
		if( (drawMask & 1) != 0 && frame.hasMask() ) {
			Arrays.fill( frame.mask.data, val );
		}
		int mask = drawMask>>1;
		for( int j = 0; j < frame.planes.size(); j++) {
			if (((1 << j) & mask) != 0) {
				Arrays.fill( frame.planes.get(j).data, val );
			}
		}
	}

	public void setMaskPixel(int x, int y, boolean b) {
		int save = drawMask;
		drawMask = 1;
		setPixel(x, y, b?1:0);
		drawMask = save;
	}

	@Override
	public String toString() {
		return String.format("DMD [width=%s, height=%s, bytesPerRow=%s, planeSize=%s, drawMask=%s, frame=%s, actualBuffer=%s, mask=%s]", width, height,
				bytesPerRow, planeSize, drawMask, frame, actualBuffer, Arrays.toString(pixmask));
	}


}
