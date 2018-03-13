package com.rinke.solutions.pinball.view.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.handler.PaletteHandler;
import com.rinke.solutions.pinball.view.model.ViewModel;

public class PaletteHandlerTest {

	PaletteHandler uut;
	private ViewModel vm;
	
	RGB[] colors = { 
			new RGB(0,0,0), new RGB(1,1,1), new RGB(2,2,2), new RGB(3,3,3),
			new RGB(4,0,0), new RGB(5,1,1), new RGB(6,2,2), new RGB(3,3,3),
			new RGB(8,0,0), new RGB(9,1,1), new RGB(10,2,2), new RGB(11,3,3),
			new RGB(12,0,0), new RGB(13,1,1), new RGB(14,2,2), new RGB(15,3,3),
			};

	@Before
	public void setUp() throws Exception {
		vm = new ViewModel();
		uut = new PaletteHandler(vm);
		uut.messageUtil = Mockito.mock(MessageUtil.class);
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
		uut.loadPalette("./src/test/resources/smartdmd.txt");
	}

	@Test
	public void testLoadPaletteXml() throws Exception {
		uut.loadPalette("./src/test/resources/defaultPalette.xml");
	}

	@Test
	public void testCopyPalettePlaneUpgrade() throws Exception {
		vm.setSelectedPalette(new Palette(colors,0,"foo"));
		uut.copyPalettePlaneUpgrade();
	}

	@Test
	public void testOnRenamePalette() throws Exception {
		vm.setSelectedPalette(new Palette(colors,2,"pal1"));
		uut.onRenamePalette("2 - foo");
		assertThat(vm.selectedPalette.name, equalTo("foo"));
		assertThat(vm.selectedPalette.index, equalTo(2));
	}

	@Test
	public void testOnRenamePaletteWrongName() throws Exception {
		vm.setSelectedPalette(new Palette(colors,2,"pal1"));
		uut.onRenamePalette("2**oo");
		assertThat(vm.selectedPalette.name, equalTo("pal1"));
		assertThat(vm.selectedPalette.index, equalTo(2));
	}
}
