package com.rinke.solutions.pinball;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Widget;

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
		if(width%8 >0) bytesPerRow++;
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
	
	public void draw( PaintEvent ev ) {
		
		Image image = new Image(ev.display,ev.width,ev.height);
		GC gcImage = new GC(image);
		
		int pitch = 7;
		int offset = 20;
		Color[] cols  = new Color[4];
		// hell ffae3a
		// 2/3 ca8a2e
		// 1/3 7f561d
		// schwarz: 191106
		cols[0] = new Color(ev.display,0x19,0x00,0x06);
		cols[1] = new Color(ev.display,0x6f,0x00,0x00);
		cols[2] = new Color(ev.display,0xca,0x00,0x00);
		cols[3] = new Color(ev.display,0xff,0x00,0x00);
		Color bg = new Color(ev.display,10,10,10);
		gcImage.setBackground(bg);
		gcImage.fillRectangle(0, 0, ev.width,ev.height);
		
		for( int row = 0; row < height; row++) {
			for( int col = 0; col<width; col++) {
				// lsb first 
				//byte mask = (byte) (1 << (col % 8));
				// hsb first
				byte mask = (byte) (128 >> (col % 8));
				int v  = 0;
				v +=  ( frame1[col/8 + row * bytesPerRow] & mask) != 0 ? 1:0;
				v +=  ( frame2[col/8 + row * bytesPerRow] & mask) != 0 ? 2:0;
				
				gcImage.setBackground(cols[v]);
				gcImage.fillOval(offset+col*pitch, offset+row*pitch, pitch, pitch);
			}
		}

		ev.gc.drawImage(image, 0, 0);
		cols[0].dispose();
		cols[1].dispose();cols[2].dispose();cols[3].dispose();
		bg.dispose();
		
		image.dispose();
        gcImage.dispose();
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

	public boolean getPixel(byte[] buffer, int x, int y) {
		int bitpos = 0;
		int yoffset = y * (width/4);
		if( y >= height/2) {
			bitpos = 4;
			yoffset = (y - height/2 ) * (width/4);
		}
		int index = yoffset + x/4;
		return (buffer[index] & ~mask[(x&3)+bitpos])==0 ;
	}
	
	public void setPixel(byte[] buffer, int x, int y, boolean on) {
		int bitpos = 0;
		int yoffset = y * (width/4);
		if( y >= height/2) {
			bitpos = 4;
			yoffset = (y - height/2 ) * (width/4);
		}
		int index = yoffset + x/4;

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

	public byte[] transformFrame1(byte[] in) {
		byte[] t = new byte[in.length];
		for(int i = 0; i<in.length;i++) {
			t[i] =  (byte) ~ (in[i]);
		}
		return t;
	}

	public byte[] transformFrame(byte[] in) {
		byte[] t = new byte[in.length];
		//for(int i=0; i<t.length;i++) t[i] = (byte) 255;
		
		for( int y=0; y<height; y++ ) {
			for( int x=0; x<width; x++ ) {
				boolean on = (in[y*bytesPerRow + x/8] & m2[x % 8]) != 0;
//				System.out.print(on?"*":".");
				setPixel(t, x, y, on );
			}
//			System.out.println("");
		}
		return t;
	}
	
	public String dumpAsCode() {
		StringBuilder builder = new StringBuilder();
		builder.append(" { ");
		byte[] f = transformFrame(frame1);
		for(int i = 0; i<f.length;i++) {
			builder.append(String.format("0x%02X , ", f[i]));
		}
		builder.append(" }, \n");
//		builder.append("byte[] f2 = new byte { \n");
//		for(int i = 0; i<frame2.length;i++) {
//			builder.append(String.format("0x%02X , ", frame2[i]));
//		}
//		builder.append("}; \n");
		return builder.toString();
	}

	public void writeTo(DataOutputStream os) throws IOException {
		os.writeShort(width);
		os.writeShort(height);
		os.writeShort(frameSizeInByte);
		os.write(frame1);
		os.write(frame2);
	}

	public static DMD read(DataInputStream is) throws IOException {
		int w = is.readShort();
		int h = is.readShort();
		DMD dmd = new DMD(w, h);
		int sizeInByte = is.readShort();
		assert( sizeInByte == dmd.getFrameSizeInByte() );
		is.read(dmd.frame1);
		is.read(dmd.frame2);
		return dmd;
	}


}
