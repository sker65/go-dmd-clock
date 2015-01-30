package com.rinke.solutions.pinball;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.rinke.solutions.pinball.renderer.FrameSet;
import com.rinke.solutions.pinball.renderer.PngRenderer;
import com.rinke.solutions.pinball.renderer.Renderer;

public class DMDClock {
	
	Renderer renderer = new PngRenderer();

	Map<Character,DMD> charMapBig = new HashMap<Character, DMD>();
	Map<Character,DMD> charMapSmall = new HashMap<Character, DMD>();

	DMD emptyBig = new DMD(16, 32);
	DMD emptySmall = new DMD(8, 9);
	
	boolean fadeIn = true;
	int renderCycles = 0;
	
	boolean showSeconds = false;

	public void restart() {
		renderCycles = 0;
	}
	
	public DMDClock(boolean showSeconds) {
		super();
		this.showSeconds = showSeconds;
		String alpha = "0123456789:";
		String base = "/home/sr/Downloads/Pinball/";
		int i = 0;
		// 352 - 35c fuer klein 6x9 
		for (int j = 0x352; j <= 0x035C; j++) {
			DMD dmd = new DMD(8, 9);
			FrameSet frameSet = renderer.convert(base + "clock", dmd,j);
			dmd.writeOr(frameSet);
			charMapSmall.put(alpha.charAt(i), dmd);
			i++;
		}
		i = 0;
		for (int j = 0x329; j <= 0x0333; j++) {
			DMD dmd = new DMD(16, 32);
			FrameSet frameSet = renderer.convert(base + "clock", dmd,j);
			dmd.writeOr(frameSet);
			charMapBig.put(alpha.charAt(i), dmd);
			i++;
		}
	}
	
	public void renderTime(DMD dmd) {
		renderTime(dmd,false,showSeconds?0:24,3);
	}
	
	public void renderTime(DMD dmd, boolean small, int x, int y) {
		int xoffset = x/8; // only byte aligned
		
		String time = String.format("%tT", Calendar.getInstance());
		if( !showSeconds ) {
			time = time.substring(0,5);
		}
		Map<Character,DMD> map = small?charMapSmall:charMapBig;
		
		for(int i = 0; i<time.length(); i++) {
			DMD src = map.get(time.charAt(i));
			if( ':' == time.charAt(i) && (System.currentTimeMillis() % 1000) < 500 ) {
				src = small?emptySmall:emptyBig;
			}
			if( src != null ) {
				boolean low = renderCycles < 1;
				copy(dmd, y, xoffset, small?emptySmall:emptyBig, low, small);
				copy(dmd, y, xoffset, src, low, small);
				xoffset += small?1:2;
			}
		}
		renderCycles++;
	}

	// TODO methode in DMD selbst
	private void copy(DMD target, int yoffset, int xoffset, DMD src, boolean low, boolean small) {
		if( small ){
			for( int row = 0; row <9; row++) {
				target.frame1[(row+yoffset)*target.getBytesPerRow()+xoffset] = src.frame1[src.getBytesPerRow()*row];
				if(!low) {
					target.frame2[(row+yoffset)*target.getBytesPerRow()+xoffset] = src.frame1[src.getBytesPerRow()*row];
				}
			}
		} else {
			for( int row = 0; row <=27; row++) {
				target.frame1[(row+yoffset)*target.getBytesPerRow()+xoffset] = src.frame1[src.getBytesPerRow()*row];
				target.frame1[(row+yoffset)*target.getBytesPerRow()+xoffset+1] = src.frame1[src.getBytesPerRow()*row+1];
				if(!low) {
					target.frame2[(row+yoffset)*target.getBytesPerRow()+xoffset] = src.frame1[src.getBytesPerRow()*row];
					target.frame2[(row+yoffset)*target.getBytesPerRow()+xoffset+1] = src.frame1[src.getBytesPerRow()*row+1];
				}
			}
		}
	}
	
	private String dumpAsCode() {
		StringBuilder sb = new StringBuilder();
		for(Entry<Character, DMD> c: charMapBig.entrySet() ) {
			sb.append("// big " +c.getKey()+"\n");
			sb.append(c.getValue().dumpAsCode());
		}
//		for(Entry<Character, DMD> c: charMapSmall.entrySet() ) {
//			sb.append("// small " +c.getKey()+"\n");
//			sb.append(c.getValue().dumpAsCode(5));
//		}
		return sb.toString();
	}
	
	public static void main( String[] args ) {
		DMDClock clock = new DMDClock(false);
		System.out.println(clock.dumpAsCode());
	}

}
