package com.rinke.solutions.pinball;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;

public class SmartDMDImporter {

    public List<Palette> importFromFile(String filename) {
        List<Palette> res = new ArrayList<>();
        try(BufferedReader reader = getReader(filename) ) {
            String line = reader.readLine();
            int numberOfPalettes = 0;
            int defaultPalette = 0;
            int persistent = 0;
            while (line != null) {
                int pos = line.indexOf("=");
                if (pos != -1) {
                    String key = line.substring(0, pos);
                    String val = line.substring(pos+1);
                    if (key.equals("dmd_npalettes")) {
                        numberOfPalettes = Integer.parseInt(val);
                    } else if (key.equals("dmd_defaultpalette")) {
                        defaultPalette = Integer.parseInt(val);
                    } else if (key.startsWith("dmd_palette")) {
                        int idx = Integer.parseInt(key.substring(11));
                        Palette p = parsePalette(val);
                        p.type = idx == defaultPalette ? PaletteType.DEFAULT :
                        	(persistent==0?PaletteType.NORMAL:PaletteType.PERSISTENT);
                        p.index = idx;
                        res.add(p);
                    } else if( key.equals("dmd_persistentpalette")) {
                    	persistent = Integer.parseInt(val);
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("error reading "+filename, e);
        } 
        return res;
    }

    // parse palette from:
    // Ship,Upscaling=2,0xFF000000,0xFFFFFFBB,0xFF804000,0xFF808000,0xFFAC0000,0xFF797979,0xFFAC5400,0xFFACACAC,0xFF545454,0xFF5454FF,0xFF0000FF,0xFF000080,0xFFFF5454,0xFF494925,0xFFFFFF54,0xFFFFFFFF

    protected Palette parsePalette(String pal) {
        String[] ptok = pal.split(",");
        List<RGB> rgb = new ArrayList<>();
        for (int i = 2; i < ptok.length; i++) {
            String t = ptok[i];
            long val = Long.parseLong(t.substring(2), 16) & 0xFFFFFF;
            rgb.add(new RGB(
                    (int)(val >> 16),
                    (int)(val >> 8) & 0xFF,
                    (int) val & 0xFF));
        }
        Palette p = new Palette(rgb.toArray(new RGB[rgb.size()]));
        p.name = ptok[0];
        return p;
    }

    protected BufferedReader getReader(String filename) throws IOException {
        return new BufferedReader(new FileReader(filename));
    }

}
