package com.rinke.solutions.pinball.view.handler;

import java.util.UUID;

import org.eclipse.jface.viewers.StructuredSelection;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class PaletteHandler extends ViewHandler {
	
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	FileChooserUtil fileChooserUtil;
	@Autowired
	FileHelper fileHelper;
	
	public PaletteHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm,m,d);
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
		for( Palette p : model.palettes ) {
			vm.palettes.add(p);
		}
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
		
	}
	
	public void onApplyPalette(Palette selectedPalette) {
		
	}
	
	public void onLoadPalette() {
		
	}
	
	public void onSavePalette() {
		
	}

	public void onUploadPalette(Palette palette) {
		
	}

	public void onSelectedPaletteChanged( Palette ov, Palette nv) {
		// forward to palette tool -> we need a boundable prop any way
	}
}
