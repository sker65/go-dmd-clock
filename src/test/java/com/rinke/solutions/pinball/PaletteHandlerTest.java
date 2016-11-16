package com.rinke.solutions.pinball;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.fappel.swt.DisplayHelper;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.RecentMenuManager;

public class PaletteHandlerTest {

	PaletteHandler uut;
	PinDmdEditor editor;
	
	@Before
	public void setUp() throws Exception {
		editor = new PinDmdEditor();
		uut = new PaletteHandler(editor, null);
		uut.editor.paletteComboViewer = Mockito.mock(ComboViewer.class);
		uut.editor.recentPalettesMenuManager = Mockito.mock(RecentMenuManager.class);
	}

	@Test
	public void testCheckOverride() throws Exception {
		List<Palette> palettes = new ArrayList<>();
		List<Palette> palettesImported = new ArrayList<>();
		String override = uut.checkOverride(palettes, palettesImported);
		assertThat(override, equalTo(""));

		palettes.add(new Palette(Palette.defaultColors(), 0, "foo"));
		palettesImported.add(new Palette(Palette.defaultColors(), 1, "foo"));
		override = uut.checkOverride(palettes, palettesImported);
		assertThat(override, equalTo(""));

		palettes.add(new Palette(Palette.defaultColors(), 2, "foo2"));
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
				editor.project.palettes.clear();
				uut.loadPalette("./src/test/resources/smartdmd.txt");
			}
	
	@Test
			public void testLoadPaletteXml() throws Exception {
				uut.loadPalette("./src/test/resources/defaultPalette.xml");
			}



}
