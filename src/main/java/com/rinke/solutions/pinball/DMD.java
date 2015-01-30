package com.rinke.solutions.pinball;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;

import com.rinke.solutions.pinball.renderer.FrameSet;

public class DMD {

	private int width;
	private int height;
	
	public byte[] frame1 = null;
	public byte[] frame2 = null;
	
	private int frameSizeInByte;
	
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

	public int getFrameSizeInByte() {
		return frameSizeInByte;
	}

	private int bytesPerRow;
	
	public int getBytesPerRow() {
		return bytesPerRow;
	}

	public DMD( int w, int h ) {
		this.width = w;
		this.height = h;
		bytesPerRow = width / 8;
		frameSizeInByte = bytesPerRow * height;
		frame1 = new byte[frameSizeInByte];
		frame2 = new byte[frameSizeInByte];
	}
	
	public DMD() {
		this(128,32);
	}

	public void copyInto(DMD src, int xsrc, int ysrc, int w, int h, int destx, int desty) {
	}
	
	public void setPixel( int col, int row, int v ) {
	}
	
	public void draw( PaintEvent e ) {
		int pitch = 7;
		int offset = 20;
		Color[] cols  = new Color[4];
		// hell ffae3a
		// 2/3 ca8a2e
		// 1/3 7f561d
		// schwarz: 191106
		cols[0] = new Color(e.display,0x19,0x00,0x06);
		cols[1] = new Color(e.display,0x6f,0x00,0x00);
		cols[2] = new Color(e.display,0xca,0x00,0x00);
		cols[3] = new Color(e.display,0xff,0x00,0x00);
		
		
		for( int row = 0; row < height; row++) {
			for( int col = 0; col<width; col++) {
				// lsb first 
				//byte mask = (byte) (1 << (col % 8));
				// hsb first
				byte mask = (byte) (128 >> (col % 8));
				int v  = 0;
				v +=  ( frame1[col/8 + row * bytesPerRow] & mask) != 0 ? 1:0;
				v +=  ( frame2[col/8 + row * bytesPerRow] & mask) != 0 ? 2:0;
				
				e.gc.setBackground(cols[v]);
				e.gc.fillOval(offset+col*pitch, offset+row*pitch, pitch, pitch);
			}
		}
		cols[0].dispose();
		cols[1].dispose();cols[2].dispose();cols[3].dispose();
	}

	public void setFrames(byte[] f1, byte[] f2) {
		this.frame1 = f1;
		this.frame2 = f2;
	}

	public void clear() {
		for(int i = 0; i < frameSizeInByte; i++) {
			frame1[i]=0; frame2[i]=0;
		}
		
	}

	public void writeOr(FrameSet frameSet) {
		if( frameSet != null ) {
		  copyOr(frame1, frameSet.frame1);
		  copyOr(frame2, frameSet.frame2);
		}
	}

	private void copyOr(byte[] target, byte[] src) {
		for (int i = 0; i < src.length; i++) {
			target[i] = (byte) (target[i] | src[i]);
		}
	}
	
	// masken zum setzen von pixeln
	int[] mask  = {
			0b01111111,
			0b11011111,
			0b11110111,
			0b11111101,

			0b10111111,
			0b11101111,
			0b11111011,
			0b11111110

	};

	void setPixel(byte[] buffer, int x, int y, boolean on) {
		int bitpos = 0;
		int yoffset = y * (width/4);
		if( y >= height/2) {
			bitpos = 4;
			yoffset = (y - height/2 ) * (width/4);
		}
		int index = yoffset + x/4;
		//System.out.println("y: "+y+", offset: "+yoffset);

		if( on ) {
			buffer[index] &=  mask[(x&3)+bitpos];
		} else {
			buffer[index] |= ~ mask[(x&3)+bitpos];
		}
	}

	byte[] m2 = { 
			(byte) 0b10000000,
			(byte) 0b01000000,
			(byte) 0b00100000,
			(byte) 0b00010000,
			(byte) 0b00001000,
			(byte) 0b00000100,
			(byte) 0b00000010,
			(byte) 0b00000001,
			};

	public byte[] transformFrame(byte[] in) {
		int l = in.length;
		byte[] t = new byte[l];
		//for(int i=0; i<t.length;i++) t[i] = (byte) 255;
		
		for( int y=0; y<height; y++ ) {
			for( int x=0; x<width; x++ ) {
				boolean on = (frame1[y*bytesPerRow + x/8] & m2[x % 8]) != 0;
//				System.out.print(on?"*":".");
				setPixel(t, x, y, on );
			}
//			System.out.println("");
		}
		return t;
	}
	
	public String dumpAsCode() {
		StringBuilder builder = new StringBuilder();
		builder.append("byte f1 =  { ");
		byte[] f = transformFrame(frame1);
		for(int i = 0; i<f.length;i++) {
			builder.append(String.format("0x%02X , ", f[i]));
		}
		builder.append(" }; \n");
//		builder.append("byte[] f2 = new byte { \n");
//		for(int i = 0; i<frame2.length;i++) {
//			builder.append(String.format("0x%02X , ", frame2[i]));
//		}
//		builder.append("}; \n");
		return builder.toString();
	}

}
