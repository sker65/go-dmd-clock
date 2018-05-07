package com.rinke.solutions.pinball.ui;


import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.fappel.swt.DisplayHelper;
import com.rinke.solutions.pinball.animation.AniReader;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Palette;

public class GifExporterSWTTest {
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();

	private GifExporter gifExporter;
	
	protected Palette palette = Palette.getDefaultPalettes().get(0);

	@Before
	public void setUp() throws Exception {
		List<Animation> anis = AniReader.readFromFile("./src/test/resources/ex1.ani");
		gifExporter = new GifExporter();
		gifExporter.setAni(anis.get(0));
		gifExporter.setPalette(palette);
		gifExporter.createContents();
		gifExporter.display = displayHelper.getDisplay();
	}

	@Test
	public void testExportAni() throws Exception {
		String filename = testFolder.newFile("1.ani").getPath();
		gifExporter.exportAni(filename, false, 20);
	}
	

}
