package com.rinke.solutions.pinball;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.renderer.PngRenderer;

public class DMDClock {
	
    private static Logger LOG = LoggerFactory.getLogger(DMDClock.class); 
    
	PngRenderer renderer = new PngRenderer();

	Map<Character,DMD> charMapBig = new HashMap<Character, DMD>();
	Map<Character,DMD> charMapSmall = new HashMap<Character, DMD>();
	Map<Character,DMD> charMapBigMask = new HashMap<Character, DMD>();
	Map<Character,DMD> charMapSmallMask = new HashMap<Character, DMD>();

	DMD emptyBig = new DMD(8, 32);
	DMD emptySmall = new DMD(8, 9);
	
	boolean fadeIn = true;
	int renderCycles = 0;
	
	boolean showSeconds = false;

	public void restart() {
		renderCycles = 0;
	}

	public void compileFontData(String filename, String fontname, String small, String base) {
		
		if( !base.endsWith("/") ) base += "/";
		int i = 0;
		// 352 - 35c fuer klein 6x9 
		// alter big font 0x352; j <= 0x035C
		for (int j = 0x013; j <= 0x021; j++) {
			DMD dmd = (j == 0x1D || j == 0x1E ? new DMD(5, 13) : new DMD(10, 13));
			Frame frame = renderer.convert(base + "fonts/"+small, dmd,j);
			dmd.writeOr(frame);
			charMapSmall.put(alpha.charAt(i), dmd);
			i++;
		}
		i = 0;
		for (int j = 0x013; j <= 0x021; j++) {
			DMD dmd = (j == 0x1D || j == 0x1E  ? new DMD(5, 13) : new DMD(10, 13));
			renderer.setPattern("Image-0x%04X-mask");
			Frame frame = renderer.convert(base + "fonts/"+small, dmd,j);
			dmd.writeOr(frame);
			charMapSmallMask.put(alpha.charAt(i), dmd);
			i++;
		}
		i = 0;
		for (int j = 0x16A; j <= 0x0178; j++) {
			DMD dmd = new DMD(16, 32);
			renderer.setOverrideDMD(true);
			renderer.setPattern("Image-0x%04X");
			Frame frame = renderer.convert(base + "fonts/"+fontname, dmd, j);
			dmd = new DMD(16, 32);
			dmd.writeOr(frame);
			charMapBig.put(alpha.charAt(i), dmd);
			i++;
		}
		i = 0;
		for (int j = 0x16A; j <= 0x0178; j++) {
			DMD dmd = new DMD(16, 32);
			renderer.setPattern("Image-0x%04X-mask");
			Frame frame = renderer.convert(base + "fonts/"+fontname, dmd,j);
			dmd = new DMD(16, 32);
			dmd.writeOr(frame);
			charMapBigMask.put(alpha.charAt(i), dmd);
			i++;
		}

		writeFontData(filename);
	}
	
	String alpha = "0123456789: .C*";
	
	public DMDClock(boolean showSeconds) {
		super();
		this.showSeconds = showSeconds;
		
		// check for compiled font first
		if( !loadFontData("font0.dat") ) {

			//compileFontData("font.dat");
			
		}
	}
	
	private boolean loadFontData(String filename) {
//		File file = new File(filename);
//		if( !file.exists() ) return false;
		DataInputStream is = null;
		try{
			InputStream stream = this.getClass().getResourceAsStream("/"+filename);
			is = new DataInputStream(stream);
			readMap(charMapBig,is);
			readMap(charMapBigMask,is);
			readMap(charMapSmall,is);
			readMap(charMapSmallMask,is);
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if( is != null ) is.close();
			} catch (IOException e) {
			}
		}
		return true;
	}

	private void readMap(Map<Character, DMD> charMap, DataInputStream is) throws IOException {
		int size = is.readByte();
		charMap.clear();
		while(size-- > 0) {
			char c = (char) is.readByte();
			int w = is.readByte();
			int h = is.readByte();
			is.readShort();
			DMD dmd = new DMD(w, h);
			is.read(dmd.getFrame().planes.get(0).data);
//			for( int i = 0; i < dmd.frame1.length; i++) {
//				dmd.frame1[i] ^= 0x00;
//			}
			charMap.put(c, dmd);
		}
	}

	private void writeFontData(String filename) {
		DataOutputStream os = null;
		try {
			os = new DataOutputStream(new FileOutputStream(filename));
			writeMap(charMapBig,os);
			writeMap(charMapBigMask,os);
			writeMap(charMapSmall,os);
			writeMap(charMapSmallMask,os);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(os != null ) os.close();
			} catch (IOException e) {
			}
		}
	}

	private void writeMap(Map<Character, DMD> charMap, DataOutputStream os) throws IOException {
		os.writeByte(charMap.size());
		for(int i = 0; i < charMap.size();i++) {
			char c = alpha.charAt(i);
			os.writeByte(c);
			DMD dmd = charMap.get(Character.valueOf(c));//.writeTo(os);
			os.writeByte(dmd.getWidth());
			os.writeByte(dmd.getHeight());
			os.writeShort(dmd.getPlaneSizeInByte());
			//os.write(dmd.transformFrame1(dmd.frame1));
			os.write(dmd.getFrame().planes.get(0).data);
		}
	}

	public void renderTime(DMD dmd,boolean mask) {
		renderTime(dmd,false,showSeconds?0:24,0,mask);
	}
	
	// upgrade to support arbitrary pos
	public void renderTime(DMD dmd, boolean small, int x, int y, boolean mask) {
		int xoffset = x/8; // only byte aligned
		
		String time = String.format("%tT", Calendar.getInstance());
		if( !showSeconds ) {
			time = time.substring(0,5);
		}
		Map<Character,DMD> map = mask ? (small?charMapSmallMask:charMapBigMask):(small?charMapSmall:charMapBig);
		
		for(int i = 0; i<time.length(); i++) {
			DMD src = map.get(time.charAt(i));
			if( ':' == time.charAt(i) && (System.currentTimeMillis() % 1000) < 500 ) {
				src = map.get(' ');
			}
			if( src != null ) {
				boolean low = renderCycles < 1;
				//copy(dmd, y, xoffset, small?emptySmall:emptyBig, low, small);
				dmd.copy( y, xoffset, src, low, mask);
				xoffset += src.getWidth()/8;
			}
		}
		renderCycles++;
	}

	/** 
	 * genutzt zum generieren der Fonts
	 * @param args
	 */
	public static void main( String[] args ) {
		DMDClock clock = new DMDClock(false);
		//System.out.println(clock.dumpAsCode());
		Properties p = new Properties();
		try {
			p.load(new FileInputStream("font.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		int i = 0;
		String small = p.getProperty("small", "small");
		String base = p.getProperty("base", ".");
		while(p.containsKey("font"+i)) {
			String fontname = p.getProperty("font"+i);
			LOG.debug("compiling font"+i+" = "+fontname);
			clock.compileFontData("font"+i+".dat", fontname, small, base);
			i++;
		}
	}

}
