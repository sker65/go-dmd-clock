package com.rinke.solutions.pinball.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;

/**
 * DMC format importer: always 12 lines, each containing
 * line by line
 * red
 * green
 * blue
 * red66
 * green66
 * blue66
 * red33
 * green33
 * blue33
 * red0
 * green0
 * blue0
 * @author stefanri
 *
 */

@Slf4j
public class DMCImporter implements PaletteImporter {
	
	/* (non-Javadoc)
	 * @see com.rinke.solutions.pinball.io.PaletteImporter#importFromFile(java.lang.String)
	 */
	@Override
	public List<Palette> importFromFile(String filename) {
		List<Palette> res = new ArrayList<>();
		log.info("importing color profile from {}", filename);
		int i = 0;
		try {
			List<String> lines = IOUtils.readLines(new FileInputStream(filename));
			RGB[] colors = new RGB[16];
			for(i=0; i<16; i++ ) colors[i] = new RGB(0,0,0);
			// convert to numbers
			int[] c = new int[lines.size()];
			i = 0;
			for (String line : lines) {
				c[i++] = Integer.parseInt(line);
			}
			colors[15] = new RGB(c[0],c[1],c[2]);
			colors[7] = new RGB(c[3],c[4],c[5]);
			colors[1] = new RGB(c[6],c[7],c[8]);
			colors[0] = new RGB(c[9],c[10],c[11]);
			
			res.add(new Palette(colors, 2, filename));
		} catch (IOException e) {
			log.error("error loading {} for dmc import", filename);
		}
		return res;
	}

}
