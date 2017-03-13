package com.rinke.solutions.pinball;

import java.io.File;
import java.util.List;

import lombok.extern.java.Log;

import com.rinke.solutions.pinball.animation.AniReader;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.model.RGB;

@Log
public class AniConverter {

	FileHelper fileHelper = new FileHelper();

	public static void main(String[] args) {
		AniConverter converter = new AniConverter();
		converter.loadProject(args[0]);
	}
	
	private void populatePalette(Animation ani, List<Palette> palettes) {
		if (ani.getAniColors() != null) {
			// if loaded colors with animations propagate as palette
			boolean colorsMatch = false;
			for (Palette p : palettes) {
				if (p.sameColors(ani.getAniColors())) {
					colorsMatch = true;
					log.info("palette match "+ani.getDesc());
					ani.setPalIndex(p.index);
					break;
				}
			}
			if (!colorsMatch) {
				Palette aniPalette = new Palette(ani.getAniColors(), palettes.size(), ani.getDesc());
				palettes.add(aniPalette);
			}
		}
	}

	
	public void convert(String src, String dest, List<Palette> palettes) {
		List<Animation> anis = AniReader.readFromFile(src);
		
		anis.stream().forEach(a->a.setProjectAnimation(true));
		
		for(Animation b : anis) {
			CompiledAnimation a = (CompiledAnimation)b;
			a.actFrame = 0;
			
			populatePalette(a, palettes);
			
			DMD tmp = new DMD(128, 32);
			for (int i = 0; i <= a.end; i++) {
				Frame frame = new Frame( a.render(tmp, false) ); // copy frames to not remove in org
				Plane plane1 = frame.planes.get(1);
				Plane plane0 = frame.planes.get(0);
				plane0.marker = 1;
				plane1.marker = 0;
				frame.planes.set(0, plane1);
				frame.planes.set(1, plane0);
				a.frames.set(a.actFrame-1, frame);
			}
			RGB[] colors = a.getAniColors(); 
			if( colors != null && colors.length >0 ) {
				switchColors(colors);
				a.setAniColors(colors);
			}
			//while( a.get)
		}
		
		for( Palette pal : palettes ) {
			switchColors(pal);
		}
	
		AniWriter.writeToFile(anis, dest, 3, palettes);
	}
	
	String replaceExtensionTo(String newExt, String filename) {
		int p = filename.lastIndexOf(".");
		if (p != -1)
			return filename.substring(0, p) + "." + newExt;
		return filename;
	}
	
	String buildRelFilename(String parent, String file) {
		if( file.contains(File.separator)) return file;
		return new File(parent).getParent() + File.separator + new File(file).getName();
	}
	
	void loadProject(String filename) {
		//log.info("load project from {}", filename);
		Project project = (Project) fileHelper.loadObject(filename);
		
		if (project != null) {

			// if inputFiles contain project filename remove it
			String aniFilename = replaceExtensionTo("ani", filename);
			
			convert(aniFilename, aniFilename+".cnv", project.palettes);

			fileHelper.storeObject(project, filename+".cnv.xml");
			
		}

	}
	
	private void switchColors(RGB[] c) {
		RGB tmp = c[2];
		c[2] = c[1];
		c[1] = tmp;
		
		tmp = c[6];
		c[6] = c[5];
		c[5] = tmp;

		tmp = c[10];
		c[10] = c[9];
		c[9] = tmp;

		tmp = c[14];
		c[14] = c[13];
		c[13] = tmp;
	}
	
	private void switchColors(Palette pal) {
		// 1,2 - 5,6 - 9,10 - 13,14 
		RGB tmp = pal.colors[2];
		pal.colors[2] = pal.colors[1];
		pal.colors[1] = tmp;
		
		tmp = pal.colors[5];
		pal.colors[5] = pal.colors[6];
		pal.colors[6] = tmp;
		
		tmp = pal.colors[9];
		pal.colors[9] = pal.colors[10];
		pal.colors[10] = tmp;
		
		tmp = pal.colors[13];
		pal.colors[13] = pal.colors[14];
		pal.colors[14] = tmp;
	}

}
