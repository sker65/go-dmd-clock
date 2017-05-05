package com.rinke.solutions.pinball;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.io.DMCImporter;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.io.PaletteImporter;
import com.rinke.solutions.pinball.io.SmartDMDImporter;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.util.FileChooserUtil;

@Slf4j
public class PaletteHandler {
	
	FileHelper fileHelper = new FileHelper();
	PinDmdEditor editor;
	FileChooserUtil fileChooserUtil;
	
	public PaletteHandler(PinDmdEditor editor, Shell shell) {
		super();
		this.editor = editor;
		fileChooserUtil = new FileChooserUtil(shell);
	}
	
	private boolean isNewPaletteName(String text) {
		for (Palette pal : editor.project.palettes) {
			if (pal.name.equals(text))
				return false;
		}
		return true;
	}
	
	public void copyPalettePlaneUpgrade() {
		String name = editor.activePalette.name;
		if (!isNewPaletteName(name)) {
			name = "new" + UUID.randomUUID().toString().substring(0, 4);
		}
		
		RGB[] actCols = editor.activePalette.colors;
		RGB[] cols = new RGB[actCols.length];
		// copy
		for( int i = 0; i< cols.length; i++) cols[i] = RGB.of(actCols[i]);
		
		cols[2] = RGB.of(actCols[4]);
		cols[3] = RGB.of(actCols[15]);
		
		Palette newPalette = new Palette(cols, editor.project.palettes.size(), name);
		for( Palette pal : editor.project.palettes ) {
			if( pal.sameColors(cols)) {
				editor.activePalette = pal;
				editor.paletteTool.setPalette(editor.activePalette);	
				editor.paletteComboViewer.setSelection(new StructuredSelection(editor.activePalette), true);
				return;
			}
		}
		
		editor.activePalette = newPalette;
		editor.project.palettes.add(editor.activePalette);
		editor.paletteTool.setPalette(editor.activePalette);
		editor.paletteComboViewer.refresh();
		editor.paletteComboViewer.setSelection(new StructuredSelection(editor.activePalette), true);
	}
	
	public void newPalette() {
		String name = editor.paletteComboViewer.getCombo().getText();
		if (!isNewPaletteName(name)) {
			name = "new" + UUID.randomUUID().toString().substring(0, 4);
		}
		editor.activePalette = new Palette(editor.activePalette.colors, editor.project.palettes.size(), name);
		editor.project.palettes.add(editor.activePalette);
		editor.paletteTool.setPalette(editor.activePalette);
		editor.paletteComboViewer.refresh();
		editor.paletteComboViewer.setSelection(new StructuredSelection(editor.activePalette), true);
	}

	public void savePalette() {
		String filename = fileChooserUtil.choose(SWT.SAVE, editor.activePalette.name, new String[] { "*.xml", "*.json" }, new String[] { "Paletten XML", "Paletten JSON" });
		if (filename != null) {
			log.info("store palette to {}", filename);
			fileHelper.storeObject(editor.activePalette, filename);
		}
	}

	public void loadPalette() {
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.xml", "*.json,", "*.txt", "*.dmc" }, new String[] { "Palette XML",
				"Palette JSON", "smartdmd", "DMC" });
		if (filename != null)
			loadPalette(filename);
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
			String override = checkOverride(editor.project.palettes, palettesImported);
			if (!override.isEmpty()) {
				MessageBox messageBox = new MessageBox(editor.shell, SWT.ICON_WARNING | SWT.OK | SWT.IGNORE | SWT.ABORT);
				messageBox.setText("Override warning");
				messageBox.setMessage("importing these palettes will override palettes: " + override + "\n");
				int res = messageBox.open();
				if (res != SWT.ABORT) {
					importPalettes(palettesImported, res == SWT.OK);
				}
			} else {
				importPalettes(palettesImported, true);
			}
			
			editor.paletteComboViewer.setSelection(new StructuredSelection(editor.activePalette));
			editor.paletteComboViewer.refresh();
			editor.recentPalettesMenuManager.populateRecent(filename);
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
		Map<Integer, Palette> map = getMap(editor.project.palettes);
		for (Palette p : palettesImported) {
			if (map.containsKey(p.index)) {
				if (override)
					map.put(p.index, p);
			} else {
				map.put(p.index, p);
			}
		}
		editor.project.palettes.clear();
		editor.project.palettes.addAll(map.values());
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
	
	private void warn(String header, String msg) {
		MessageBox messageBox = new MessageBox(editor.shell, SWT.ICON_WARNING | SWT.OK);
		messageBox.setText(header);
		messageBox.setMessage(msg);
		messageBox.open();
	}
	
	public void onDeletePalette() {
		if( editor.activePalette != null && editor.project.palettes.size()>1 ) {
			// check if any scene is using this
			List<String> res = new ArrayList<>();
			for( Animation a: editor.scenes.values()) {
				if( a.getPalIndex() == editor.activePalette.index ) {
					res.add(a.getDesc());
				}
			}
			for( Animation a: editor.recordings.values()) {
				if( a.getPalIndex() == editor.activePalette.index ) {
					res.add(a.getDesc());
				}
			}
			if( res.isEmpty() ) {
				editor.project.palettes.remove(editor.activePalette);
				// ensure there is a default palette
				int c = 0;
				for( Palette p : editor.project.palettes) {
					if( p.type == PaletteType.DEFAULT ) c++;
				}
				if( c == 0 ) editor.project.palettes.get(0).type = PaletteType.DEFAULT;
				editor.paletteComboViewer.setSelection(new StructuredSelection(editor.project.palettes.get(0)));
				editor.paletteComboViewer.refresh();
			} else {
				warn("Palette cannot deleted", "Palette cannot deleted because it is used by: "+res);
			}
		}
	}

}
