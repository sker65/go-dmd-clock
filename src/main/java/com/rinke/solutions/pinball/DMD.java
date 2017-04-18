package com.rinke.solutions.pinball;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;

@Slf4j
public class DMD extends Observable {

    private int width;
    private int height;
    private int drawMask = 0xFFFF; // limits drawing (setPixel) to planes, that are not masked
    
    private Frame frame = new Frame();
    
    public Map<Integer,Frame> buffers = new HashMap<>();
    
    int numberOfPlanes = 2;
    int actualBuffer = 0;

    private int planeSizeInByte;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getPlaneSizeInByte() {
        return planeSizeInByte;
    }

    private int bytesPerRow;

    public int getBytesPerRow() {
        return bytesPerRow;
    }

    public void copyLastBuffer() {
    	if( actualBuffer>0) {
        	//Frame target = buffers.get(actualBuffer);
    		log.trace("copy buffer {} -> {}, max: {}", actualBuffer-1, actualBuffer, buffers.size());
        	frame = new Frame(buffers.get(actualBuffer-1));
        	buffers.put(actualBuffer, frame);
    	}
    }
    
    public void addUndoBuffer() {
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

    public DMD(int w, int h) {
        this.width = w;
        this.height = h;
        bytesPerRow = width / 8;
        if (width % 8 > 0)
            bytesPerRow++;
        planeSizeInByte = bytesPerRow * height;
        setNumberOfSubframes(2);
        buffers.put(actualBuffer, frame);
        setChanged();
    }
    
    public void setNumberOfSubframes(int n) {
        frame.planes.clear();
    	for(int i = 0; i < n; i++) {
    		frame.planes.add( new Plane((byte)i,new byte[planeSizeInByte]));
        }
        numberOfPlanes = n;
        log.trace("dmd setNumberOfSubframes {}", n);
    }

    public DMD() {
        this(PinDmdEditor.DMD_WIDTH, PinDmdEditor.DMD_HEIGHT);
    }

    public void setPixel(int x, int y, int v) {
        if( x<0 || y<0 || x>=width || y >= height ) return;
        v <<= drawShift;
    	int numberOfPlanes = frame.planes.size();
    	byte mask = (byte) (width >> (x % 8));
    	for(int plane = 0; plane < numberOfPlanes; plane++) {
    		if( ((1<<plane) & drawMask) != 0) {
        		if( (v & 0x01) != 0) {
        			frame.planes.get(plane).plane[y*bytesPerRow+x/8] |= mask;
        		} else {
        			frame.planes.get(plane).plane[y*bytesPerRow+x/8] &= ~mask;
        		}
    		}
    		v >>= 1;
    	}
    }
    
    public int getPixelWithoutMask(int x, int y) {
    	byte mask = (byte) (getWidth() >> (x % 8));
    	int v = 0;
    	for(int plane = 0; plane < frame.planes.size(); plane++) {
    		v += (frame.planes.get(plane).plane[x / 8 + y * bytesPerRow] & mask) != 0 ? (1<<plane) : 0;
    	}
    	return v;
    }
   


    public int getPixel(int x, int y) {
    	byte mask = (byte) (getWidth() >> (x % 8));
    	int v = 0;
    	for(int plane = 0; plane < frame.planes.size(); plane++) {
    		if( ((1<<plane) & drawMask) != 0) {
    			v += (frame.planes.get(plane).plane[x / 8 + y * bytesPerRow] & mask) != 0 ? (1<<plane) : 0;
    		}
    	}
    	v >>= drawShift;
    	return v;
    }
   
    public void clear() {
    	updateActualBuffer(0);
    	removeMask();
        for (Plane p : frame.planes) {
			Arrays.fill(p.plane, (byte)0);
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
        			frame.planes.add(new Plane(src.planes.get(i++).marker,new byte[bytesPerRow*height]));
        		}
        		//numberOfPlanes = frame.planes.size();
        		for ( i = 0; i < src.planes.size(); i++) {
					copyOr(frame.planes.get(i).plane,src.planes.get(i).plane);
				}
        	//s}
        }
    }

    public void writeAnd(byte[] mask) {
        if (mask != null) {
        	for(Plane p:frame.planes) {
        		copyAnd(p.plane, mask);
        	}
        }
    }
    
    public void writeNotAnd(byte[] mask) {
        if (mask != null) {
        	for(Plane p:frame.planes) {
        		copyNotAnd(p.plane, mask);
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
    
    public void copy(int yoffset, int xoffset, DMD src, boolean low, boolean mask) {
    	byte[] frame1 = frame.getPlaneBytes(0);
    	byte[] frame2 = frame.getPlaneBytes(1);
    	
        for (int row = 0; row < src.getHeight(); row++) {
            for (int col = 0; col < src.getWidth() / 8; col++) {
                if(mask) 
                    frame1[(row + yoffset) * getBytesPerRow() + xoffset + col] &= 
                        ~src.frame.getPlaneBytes(0)[src.getBytesPerRow() * row + col];
                else
                    frame1[(row + yoffset) * getBytesPerRow() + xoffset + col] = 
                        src.frame.getPlaneBytes(0)[src.getBytesPerRow() * row + col];
                
                if (!low) {
                    if( mask )
                        frame2[(row + yoffset) * getBytesPerRow() + xoffset + col] &= 
                            ~src.frame.getPlaneBytes(0)[src.getBytesPerRow() * row + col];
                    else
                        frame2[(row + yoffset) * getBytesPerRow() + xoffset + col] = 
                            src.frame.getPlaneBytes(0)[src.getBytesPerRow() * row + col];

                }
            }
        }
    }
	

    // masken zum setzen von pixeln
    int[] mask = { 
            0b01111111, 0b11011111, 0b11110111, 0b11111101,
            0b10111111, 0b11101111, 0b11111011, 0b11111110
    };

    public boolean getPixel(byte[] buffer, int x, int y) {
        int bitpos = 0;
        int yoffset = y * (width / 4);
        if (y >= height / 2) {
            bitpos = 4;
            yoffset = (y - height / 2) * (width / 4);
        }
        int index = yoffset + x / 4;
        return (buffer[index] & ~mask[(x & 3) + bitpos]) == 0;
    }

    private void setPixel(byte[] buffer, int x, int y, boolean on) {
        int bitpos = 0;
        int yoffset = y * (width / 4);
        if (y >= height / 2) {
            bitpos = 4;
            yoffset = (y - height / 2) * (width / 4);
        }
        int index = yoffset + x / 4;

        if (on) {
            buffer[index] &= mask[(x & 3) + bitpos];
        } else {
            buffer[index] |= ~mask[(x & 3) + bitpos];
        }
    }

    byte[] m2 = { (byte) 0b10000000, (byte) 0b01000000, (byte) 0b00100000, (byte) 0b00010000, (byte) 0b00001000,
            (byte) 0b00000100, (byte) 0b00000010, (byte) 0b00000001, };
	private int drawShift;

    public byte[] transformFrame1(byte[] in) {
        byte[] t = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            t[i] = (byte) ~(in[i]);
        }
        return t;
    }

    public byte[] transformFrame(byte[] in) {
        byte[] t = new byte[in.length];
        // for(int i=0; i<t.length;i++) t[i] = (byte) 255;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean on = (in[y * bytesPerRow + x / 8] & m2[x % 8]) != 0;
                // System.out.print(on?"*":".");
                setPixel(t, x, y, on);
            }
            // System.out.println("");
        }
        return t;
    }

    public Frame getFrame() {
        return frame;
    }

	public void updateActualBuffer(int i) {
		this.actualBuffer = i;
		frame = buffers.get(actualBuffer);
		log.trace("actual buffer is: {}, {}", actualBuffer, frame);
    	setChanged();
    	notifyObservers();
	}

	/*public List<byte[]> getActualBuffers() {
		return buffers.get(actualBuffer);
	}*/

	@Override
	public String toString() {
		return "DMD [width=" + width + ", height=" + height
				+ ", numberOfPlanes=" + numberOfPlanes
				+ ", actualBuffer=" + actualBuffer + "]";
	}

    public int getNumberOfPlanes() {
        return numberOfPlanes;
    }

	public int getDrawMask() {
		return drawMask;
	}

	/**
	 * draw shift controls a shift of pixel color to set. this is used to control drawing on frames containing
	 * a mask plane (which is always the first plane).
	 * so if draw shift is 1 you will draw only in the color planes
	 * TODO this should be abstracted and handled internally
	 * @param shift
	 */
	public void setDrawShift(int shift) {
		 this.drawShift = shift;
		 log.debug("setting draw shift to {}", Integer.toString(shift));
	}

	public void setDrawMask(int drawMask) {
		 this.drawMask = drawMask;
		 log.debug("setting draw mask to {}", Integer.toBinaryString(drawMask));
	}

	public void setFrame(Frame frame) {
		this.frame = frame;
		buffers.put(actualBuffer, frame);
		setChanged();
    	notifyObservers();
	}
	
	public boolean hasMask() {
		return frame.containsMask();
	}

	public void removeMask() {
		if( frame.containsMask()) {
			frame.planes.remove(0);
		}
	}

	public void ensureMask(byte[] data) {
		if(!frame.containsMask()) {
			frame.planes.add(0, new Plane(Plane.MASK, data));
		}
		System.arraycopy(data, 0, frame.getPlaneBytes(0), 0, data.length);
	}

}
