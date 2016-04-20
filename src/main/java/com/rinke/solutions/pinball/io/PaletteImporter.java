package com.rinke.solutions.pinball.io;

import java.util.List;

import com.rinke.solutions.pinball.model.Palette;

public interface PaletteImporter {

	public abstract List<Palette> importFromFile(String filename);

}