package com.rinke.solutions.pinball.io;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;

public class DMCImporterTest {
	
	DMCImporter uut;

	@Before
	public void setUp() throws Exception {
		uut = new DMCImporter();
	}

	@Test
	public void testImportFromFile() throws Exception {
		List<Palette> palettes = uut.importFromFile("src/test/resources/acd_165.dmc");
		assertEquals(1, palettes.size());
		assertEquals(new RGB(255,0,0), palettes.get(0).colors[15]);
		assertEquals(new RGB(225,15,193), palettes.get(0).colors[7]);
	}
	

}
