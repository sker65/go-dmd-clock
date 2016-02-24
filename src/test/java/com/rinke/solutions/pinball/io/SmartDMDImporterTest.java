package com.rinke.solutions.pinball.io;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.io.SmartDMDImporter;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.RGB;


public class SmartDMDImporterTest {

    SmartDMDImporter uut;
    
    @Before
    public void setup() {
        uut = new SmartDMDImporter() {

            @Override
            protected BufferedReader getReader(String filename) throws IOException {
                InputStream in = this.getClass().getResourceAsStream("/"+filename);
                return new BufferedReader(new InputStreamReader(in, "UTF-8"));
            }
            
        };
    }
    
    @Test
    public void testImportFromFile() throws Exception {
        List<Palette> p = uut.importFromFile("smartdmd.txt");
        assertEquals(15, p.size());
    }

    @Test
    public void testImportFromFile2() throws Exception {
        List<Palette> p = uut.importFromFile("smartdmd-acdc.txt");
        assertEquals(15, p.size());
    }
    
    @Test
    public void testImportFromFile3() throws Exception {
        List<Palette> list = uut.importFromFile("SmartDmd2.txt");
        assertEquals(52, list.size());
        Palette p = list.get(0);
        assertEquals("Default", p.name);
        assertEquals(new RGB(0,0,0), p.colors[0]);
        assertEquals(new RGB(0x4a,0x4a,0x4a), p.colors[1]);
        p = list.get(4);
        assertEquals("Sparky MB Intro 1", p.name);
        assertEquals(new RGB(0,0,0), p.colors[0]);
        assertEquals(new RGB(0x4a,0x4a,0x4a), p.colors[1]);
        assertEquals(PaletteType.NORMAL, p.type);
        
        assertEquals(PaletteType.DEFAULT, list.get(11).type);
        assertEquals(PaletteType.NORMAL, list.get(0).type);
    }

    String pal = "Ship,Upscaling=2,0xFF000000,0xFFFFFFBB,0xFF804000,0xFF808000,0xFFAC0000,0xFF797979,0xFFAC5400,0xFFACACAC,0xFF545454,0xFF5454FF,0xFF0000FF,0xFF000080,0xFFFF5454,0xFF494925,0xFFFFFF54,0xFFFFFFFF";
    
    @Test
    public void testParsePalette() throws Exception {
        Palette p = uut.parsePalette(pal);
        assertEquals("Ship", p.name);
        assertEquals(0, p.colors[0].red);
        assertEquals(255, p.colors[1].red);
        assertEquals(0xBB, p.colors[1].blue);
    }

}
