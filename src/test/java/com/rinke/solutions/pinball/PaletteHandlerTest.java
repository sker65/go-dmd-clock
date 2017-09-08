package com.rinke.solutions.pinball;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ComboViewer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.util.RecentMenuManager;
import com.rinke.solutions.pinball.widget.PaletteTool;

public class PaletteHandlerTest {

	PaletteHandler uut;
	PinDmdEditor editor;

	@Before
	public void setUp() throws Exception {
		editor = new PinDmdEditor();
		uut = new PaletteHandler(editor, null);
		uut.editor.paletteComboViewer = mock(ComboViewer.class);
		uut.editor.recentPalettesMenuManager = mock(RecentMenuManager.class);
		uut.editor.paletteTool = mock(PaletteTool.class);
	}

	@Test
	public void testCheckOverride() throws Exception {
		Map<Integer,Palette> palettes = new HashMap<>();
		List<Palette> palettesImported = new ArrayList<>();
		String override = uut.checkOverride(palettes, palettesImported);
		assertThat(override, equalTo(""));

		palettes.put(0,new Palette(Palette.defaultColors(), 0, "foo"));
		palettesImported.add(new Palette(Palette.defaultColors(), 1, "foo"));
		override = uut.checkOverride(palettes, palettesImported);
		assertThat(override, equalTo(""));

		palettes.put(2,new Palette(Palette.defaultColors(), 2, "foo2"));
		palettesImported.add(new Palette(Palette.defaultColors(), 2, "foo2"));
		override = uut.checkOverride(palettes, palettesImported);
		assertThat(override, equalTo("2, "));
	}

	@Test
	public void testImportPalettes() throws Exception {
		List<Palette> palettesImported = new ArrayList<Palette>();
		palettesImported.add(new Palette(Palette.defaultColors(), 0, "foo"));
		uut.importPalettes(palettesImported, false);
		uut.importPalettes(palettesImported, true);
	}

	@Test
	public void testLoadPaletteString() throws Exception {
		editor.project.paletteMap.clear();
		uut.loadPalette("./src/test/resources/smartdmd.txt");
	}

	@Test
	public void testLoadPaletteXml() throws Exception {
		uut.loadPalette("./src/test/resources/defaultPalette.xml");
	}

	@Test
	public void testCopyPalettePlaneUpgrade() throws Exception {
		RGB[] colors = { 
				new RGB(0,0,0), new RGB(1,1,1), new RGB(2,2,2), new RGB(3,3,3),
				new RGB(4,0,0), new RGB(5,1,1), new RGB(6,2,2), new RGB(3,3,3),
				new RGB(8,0,0), new RGB(9,1,1), new RGB(10,2,2), new RGB(11,3,3),
				new RGB(12,0,0), new RGB(13,1,1), new RGB(14,2,2), new RGB(15,3,3),
				};
		uut.editor.activePalette = new Palette(colors,0,"foo");
		uut.copyPalettePlaneUpgrade();
	}

}
