package com.rinke.solutions.pinball.model;

import java.util.List;

public class Project {
	public String inputFile;
	public List<Palette> palettes;
	public List<PalMapping> palMappings;
	public Project(String inputFile, List<Palette> palettes,
			List<PalMapping> palMappings) {
		super();
		this.inputFile = inputFile;
		this.palettes = palettes;
		this.palMappings = palMappings;
	}
	@Override
	public String toString() {
		return "Project [inputFile=" + inputFile + ", palettes=" + palettes
				+ ", palMappings=" + palMappings + "]";
	}
	
}
