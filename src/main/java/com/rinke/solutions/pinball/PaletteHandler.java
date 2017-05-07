package com.rinke.solutions.pinball;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.io.DMCImporter;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.io.PaletteImporter;
import com.rinke.solutions.pinball.io.SmartDMDImporter;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;

@Slf4j
@Bean
public class PaletteHandler extends Observable {
	
	@Autowired
	FileHelper fileHelper;
	
	PinDmdEditor editor;
	
	@Autowired
	FileChooserUtil fileChooserUtil;
	@Autowired
	MessageUtil messageUtil;
	
	private Palette activePalette;

	List<Palette> palettes;
	
	@Autowired
	ViewModel viewModel;
	
	public PaletteHandler(PinDmdEditor editor, Shell shell) {
		super();
		this.editor = editor;
	}
	
	private boolean isNewPaletteName(String text) {
		for (Palette pal : palettes) {
			if (pal.name.equals(text))
				return false;
		}
		return true;
	}
	
	public void copyPalettePlaneUpgrade() {
		String name = activePalette.name;
		if (!isNewPaletteName(name)) {
			name = "new" + UUID.randomUUID().toString().substring(0, 4);
		}
		
		RGB[] actCols = activePalette.colors;
		RGB[] cols = new RGB[actCols.length];
		// copy
		for( int i = 0; i< cols.length; i++) cols[i] = RGB.of(actCols[i]);
		
		cols[2] = RGB.of(actCols[4]);
		cols[3] = RGB.of(actCols[15]);
		
		Palette newPalette = new Palette(cols, palettes.size(), name);
		for( Palette pal : palettes ) {
			if( pal.sameColors(cols)) {
				activePalette = pal;
				return;
			}
		}
		
		setActivePalette(newPalette);
		palettes.add(activePalette);
	}
	
	public void onNewPalette() {
		String name = this.viewModel.getPaletteName();
		log.info("onNewPalette name={}", name);
		if (!isNewPaletteName(name)) {
			name = "new" + UUID.randomUUID().toString().substring(0, 4);
		}
		Palette palette = new Palette(activePalette.colors, palettes.size(), name);
		setActivePalette(palette);
		palettes.add(palette);
	}

	public void savePalette() {
		String filename = fileChooserUtil.choose(SWT.SAVE, activePalette.name, new String[] { "*.xml", "*.json" }, new String[] { "Paletten XML", "Paletten JSON" });
		if (filename != null) {
			log.info("store palette to {}", filename);
			fileHelper.storeObject(activePalette, filename);
		}
	}

	public String loadPalette() {
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.xml", "*.json,", "*.txt", "*.dmc" }, new String[] { "Palette XML",
				"Palette JSON", "smartdmd", "DMC" });
		if (filename != null) {
			loadPalette(filename);
			return filename;
		}
		return null;
	}

	void loadPalette(String filename) {
		java.util.List<Palette> palettesImported = null;
		if (filename.toLowerCase().endsWith(".txt") || filename.toLowerCase().endsWith(".dmc")) {
			palettesImported = getImporterByFilename(filename).importFromFile(filename);
		} else {
			Palette pal = (Palette) fileHelper.loadObject(filename);
			log.info("load palette from {}", filename);
			palettesImported = Arrays.asList(pal);
		}
		if( palettesImported != null ) {
			String override = checkOverride(palettes, palettesImported);
			if (!override.isEmpty()) {
				int res = messageUtil.warn( SWT.ICON_WARNING | SWT.OK | SWT.IGNORE | SWT.ABORT, "Override warning", "importing these palettes will override palettes: " + override + "\n");
				if (res != SWT.ABORT) {
					importPalettes(palettesImported, res == SWT.OK);
				}
			} else {
				importPalettes(palettesImported, true);
			}
		}
	}

	private PaletteImporter getImporterByFilename(String filename) {
		if (filename.toLowerCase().endsWith(".txt")) {
			return new SmartDMDImporter();
		} else if (filename.toLowerCase().endsWith(".dmc")) {
			return new DMCImporter();
		}
		return null;
	}
	
	void importPalettes(java.util.List<Palette> palettesImported, boolean override) {
		Map<Integer, Palette> map = getMap(palettes);
		for (Palette p : palettesImported) {
			if (map.containsKey(p.index)) {
				if (override)
					map.put(p.index, p);
			} else {
				map.put(p.index, p);
			}
		}
		palettes.clear();
		palettes.addAll(map.values());
	}

	String checkOverride(java.util.List<Palette> palettes2, java.util.List<Palette> palettesImported) {
		StringBuilder sb = new StringBuilder();
		Map<Integer, Palette> map = getMap(palettes2);
		for (Palette pi : palettesImported) {
			if (pi.index != 0 && map.containsKey(pi.index)) {
				sb.append(pi.index + ", ");
			}
		}
		return sb.toString();
	}

	private Map<Integer, Palette> getMap(java.util.List<Palette> palettes) {
		Map<Integer, Palette> res = new HashMap<>();
		for (Palette p : palettes) {
			res.put(p.index, p);
		}
		return res;
	}
	
	public void onDeletePalette() {
		if( activePalette != null && palettes.size()>1 ) {
			// check if any scene is using this
			List<String> res = new ArrayList<>();
			for( Animation a: editor.scenes.values()) {
				if( a.getPalIndex() == activePalette.index ) {
					res.add(a.getDesc());
				}
			}
			for( Animation a: editor.recordings.values()) {
				if( a.getPalIndex() == activePalette.index ) {
					res.add(a.getDesc());
				}
			}
			if( res.isEmpty() ) {
				palettes.remove(activePalette);
				// ensure there is a default palette
				int c = 0;
				for( Palette p : palettes) {
					if( p.type == PaletteType.DEFAULT ) c++;
				}
				if( c == 0 ) palettes.get(0).type = PaletteType.DEFAULT;
				setActivePalette(palettes.get(0));
			} else {
				messageUtil.warn("Palette cannot deleted", "Palette cannot deleted because it is used by: "+res);
			}
		}
	}

	public void setActivePalette(Palette palette) {
		if( !palette.equals(activePalette) ) {
			this.activePalette = palette;
			setChanged();
			notifyObservers();
		}
	}

	public Palette getActivePalette() {
		return this.activePalette;
	}

	public void setViewModel(ViewModel viewModel) {
		 this.viewModel = viewModel;
	}

	public void setPalettes(List<Palette> palettes) {
		 this.palettes = palettes;
	}

}
