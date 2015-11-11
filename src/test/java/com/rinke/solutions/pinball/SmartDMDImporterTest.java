package com.rinke.solutions.pinball;

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

import com.rinke.solutions.pinball.model.Palette;


public class SmartDMDImporterTest {

    SmartDMDImporter uut;
    
    @Before
    public void setup() {
        uut = new SmartDMDImporter() {

            @Override
            protected BufferedReader getReader(String filename) throws IOException {
                InputStream in = this.getClass().getResourceAsStream("/smartdmd.txt");
                return new BufferedReader(new InputStreamReader(in, "UTF-8"));
            }
            
        };
    }
    
    @Test
    public void testImportFromFile() throws Exception {
        List<Palette> p = uut.importFromFile("foo");
        assertEquals(15, p.size());
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
