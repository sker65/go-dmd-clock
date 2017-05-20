package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.io.DMCImporter;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.io.PaletteImporter;
import com.rinke.solutions.pinball.io.SmartDMDImporter;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.TypedLabel;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.view.swt.RecentMenuManager;

@Bean
@Slf4j
public class PaletteHandler extends ViewHandler {
	
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	FileChooserUtil fileChooserUtil;
	@Autowired
	FileHelper fileHelper;
	@Autowired
	RecentMenuManager recentPalettesMenuManager;
	
	public PaletteHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm,m,d);
		model.palettes.addObserver((o,a)->populate());
	}

	private boolean isNewPaletteName(String text) {
		for (Palette pal : model.palettes) {
			if (pal.name.equals(text))
				return false;
		}
		return true;
	}
	
	public void populate() {
		vm.palettes.clear();
		vm.palettes.addAll(model.palettes);
	}
	
	public void copyPalettePlaneUpgrade() {
		String name = vm.selectedPalette.name;
		if (!isNewPaletteName(name)) {
			name = "new" + UUID.randomUUID().toString().substring(0, 4);
		}
		
		RGB[] actCols = vm.selectedPalette.colors;
		RGB[] cols = new RGB[actCols.length];
		// copy
		for( int i = 0; i< cols.length; i++) cols[i] = RGB.of(actCols[i]);
		
		cols[2] = RGB.of(actCols[4]);
		cols[3] = RGB.of(actCols[15]);
		
		Palette newPalette = new Palette(cols, vm.palettes.size(), name);
		for( Palette pal : (List<Palette>)vm.palettes ) {
			if( pal.sameColors(cols)) {
				vm.setSelectedPalette(pal);
				return;
			}
		}
		vm.palettes.add(newPalette);
		vm.setSelectedPalette(newPalette);
	}

	
	public void onNewPalette() {
		String name = vm.editedPaletteName;
		if (!isNewPaletteName(name)) {
			name = "new" + UUID.randomUUID().toString().substring(0, 4);
		}
		Palette newPalette = new Palette(vm.selectedPalette.colors, vm.palettes.size(), name);
		vm.palettes.add(newPalette);
		vm.setSelectedPalette(newPalette);
	}
	
	public void onRenamePalette(Palette selectedPalette) {
		
	}

	public void onDeletePalette(Palette selectedPalette) {
		if( selectedPalette != null && vm.palettes.size()>1 ) {
			// check if any scene is using this
			
			List<String> res = searchUsingAnimations(selectedPalette);
			if( res.isEmpty() ) {
				vm.palettes.remove(selectedPalette);
				// ensure there is a default palette
				int c = 0;
				for( Palette p : (List<Palette>)vm.palettes) {
					if( p.type == PaletteType.DEFAULT ) c++;
				}
				if( c == 0 ) ((Palette)vm.palettes.get(0)).type = PaletteType.DEFAULT;
				vm.setSelectedPalette((Palette) vm.palettes.get(0));
			} else {
				messageUtil.warn("Palette cannot deleted", "Palette cannot deleted because it is used by: "+res);
			}
		}
		
	}

	List<String> searchUsingAnimations(Palette selectedPalette) {
		List<String> res = new ArrayList<>();
		for( CompiledAnimation a : vm.scenes) {
			if( a.getPalIndex() == selectedPalette.index ) {
				res.add(a.getDesc());
			}
		}
		for( Animation a: vm.recordings) {
			if( a.getPalIndex() == selectedPalette.index ) {
				res.add(a.getDesc());
			}
		}
		return res;
	}
	
	public void onApplyPalette(Palette selectedPalette) {
		log.info("change palette index in Keyframe {} to {}", vm.selectedKeyFrame.name, selectedPalette.index);

		Optional.ofNullable(vm.selectedKeyFrame).ifPresent(p->p.palIndex = selectedPalette.index);
		// change palette in ANI file
		Optional.ofNullable(vm.selectedScene).ifPresent(s->s.setPalIndex(selectedPalette.index));
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
		Map<Integer, Palette> map = getMap(vm.palettes);
		for (Palette p : palettesImported) {
			if (map.containsKey(p.index)) {
				if (override)
					map.put(p.index, p);
			} else {
				map.put(p.index, p);
			}
		}
		vm.palettes.clear();
		vm.palettes.addAll(map.values());
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
			String override = checkOverride(vm.palettes, palettesImported);
			if (!override.isEmpty()) {
				int res = messageUtil.warn( SWT.ICON_WARNING | SWT.OK | SWT.IGNORE | SWT.ABORT, "Override warning", "importing these palettes will override palettes: " + override + "\n");
				if (res != SWT.ABORT) {
					importPalettes(palettesImported, res == SWT.OK);
				}
			} else {
				importPalettes(palettesImported, true);
			}		
			recentPalettesMenuManager.populateRecent(filename);
		}
	}

	public void onLoadPalette() {
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.xml", "*.json,", "*.txt", "*.dmc" }, new String[] { "Palette XML",
				"Palette JSON", "smartdmd", "DMC" });
		if (filename != null)
			loadPalette(filename);
	}
	
	public void onSavePalette() {
		String filename = fileChooserUtil.choose(SWT.SAVE, vm.selectedPalette.name, new String[] { "*.xml", "*.json" }, new String[] { "Paletten XML", "Paletten JSON" });
		if (filename != null) {
			log.info("store palette to {}", filename);
			fileHelper.storeObject(vm.selectedPalette, filename);
		}
	}

	public void onSelectedPaletteChanged( Palette ov, Palette nv) {
		// forward to palette tool -> we need a boundable prop any way
		vm.setSelectedPaletteType(nv== null?null:nv.type);
	}

	public Palette getPaletteByIndex(int palIndex) {
		for( Palette p : (List<Palette>)vm.palettes) {
			if( p.index == palIndex ) return p;
		}
		return null;
	}
}
