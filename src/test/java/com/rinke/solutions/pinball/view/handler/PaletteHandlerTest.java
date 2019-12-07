package com.rinke.solutions.pinball.view.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.ui.PalettePicker;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.view.handler.PaletteHandler;
import com.rinke.solutions.pinball.view.model.ViewModel;

@RunWith(MockitoJUnitRunner.class)
public class PaletteHandlerTest extends HandlerTest  {

	@Mock MessageUtil messageUtil;
	@Mock FileChooserUtil fileChooserUtil;
	@Mock PalettePicker palettePicker;
	
	@InjectMocks
	PaletteHandler uut = new PaletteHandler(vm);
	
	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();
	
	RGB[] colors = { 
			new RGB(0,0,0), new RGB(1,1,1), new RGB(2,2,2), new RGB(3,3,3),
			new RGB(4,0,0), new RGB(5,1,1), new RGB(6,2,2), new RGB(3,3,3),
			new RGB(8,0,0), new RGB(9,1,1), new RGB(10,2,2), new RGB(11,3,3),
			new RGB(12,0,0), new RGB(13,1,1), new RGB(14,2,2), new RGB(15,3,3),
			};

	@Before
	public void setUp() throws Exception {
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
		uut.copyPalettePlaneUpgrade(null);
	}

	@Test
	public void testOnRenamePalette() throws Exception {
		Palette p = new Palette(colors,2,"pal1");
		vm.paletteMap.clear();
		vm.paletteMap.put(p.index, p);
		vm.setSelectedPalette(p);
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

	@Test
	public void testOnSelectedPaletteTypeChanged() throws Exception {
		uut.onSelectedPaletteTypeChanged(PaletteType.DEFAULT, PaletteType.NORMAL);
	}

	@Test
	public void testOnSelectedPaletteChanged() throws Exception {
		Palette pal1 = new Palette(colors, 1, "pal1");
		Palette pal2 = new Palette(colors, 2, "pal2");
		uut.onSelectedPaletteChanged(pal1, pal2);
	}

	@Test
	public void testOnNewPalette() throws Exception {
//		uut.onNewPalette();
	}

	@Test
	public void testOnSavePalette() throws Exception {
		Palette pal1 = new Palette(colors, 1, "pal1");
		vm.setSelectedPalette(pal1);
		String file = tmpFolder.newFile("pal.json").getAbsolutePath();
		when(fileChooserUtil.choose(anyInt(), anyString(), anyObject(), anyObject() )).thenReturn(file);
		uut.onSavePalette();
	}

	@Test
	public void testOnDeletePalette() throws Exception {
		Palette pal1 = new Palette(colors, 1, "pal1");
		vm.setSelectedPalette(pal1);
		vm.paletteMap.put(pal1.index, pal1);
		uut.onDeletePalette();
	}

	@Test
	public void testOnPickPalette() throws Exception {
		vm.selectedScene = getScene("foo");
		uut.onPickPalette();
	}

	@Test
	public void testExtractColorsFromScene() throws Exception {
		uut.extractColorsFromScene(getScene("f"), 20);
	}

	@Test
	public void testExtractColorsFromFrame() throws Exception {
		uut.extractColorsFromFrame(vm.dmd, 20);
	}

	@Test
	public void testOnExtractPalColorsFromFrame() throws Exception {
		uut.onExtractPalColorsFromFrame();
	}

	@Test
	public void testSwap() throws Exception {
		int planeSize = vm.dmdSize.planeSize;
		Frame frame = new Frame(new byte[planeSize], new byte[planeSize]);
		uut.swap(frame, 0, 1, vm.dmdSize.width, vm.dmdSize.height);
	}
}
