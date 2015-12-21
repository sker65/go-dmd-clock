package com.rinke.solutions.pinball.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Project implements Model {
	public byte version;
	public List<String> inputFiles;
	public List<Palette> palettes;
	public List<PalMapping> palMappings;
	public List<Scene> scenes;
	public Map<String,FrameSeq> frameSeqMap;
	public String name;
	
	public boolean dirty;
	
	public Project(int version, String inputFile, List<Palette> palettes,
			List<PalMapping> palMappings) {
		super();
		this.version = (byte)version;
		this.inputFiles = new ArrayList<String>();
		inputFiles.add(inputFile);
		this.palettes = palettes;
		this.palMappings = palMappings;
		this.frameSeqMap = new HashMap<String, FrameSeq>();
	}
	
	public Project() {
        version = 1;
        palettes = new ArrayList<Palette>();
        palettes.add(new Palette(Palette.defaultColors(), 0, "default"));
        palMappings = new ArrayList<>();
        scenes = new ArrayList<>();
        inputFiles=new ArrayList<>();
        frameSeqMap = new HashMap<>();
    }
	
    @Override
	public String toString() {
		return "Project [version=" + version + ", inputFiles=" + inputFiles
				+ ", palettes=" + palettes + ", palMappings=" + palMappings
				+ "]";
	}
	
	public void writeTo(DataOutputStream os) throws IOException {
		os.writeByte(version);
		os.writeShort(palettes.size());
		for(Palette p: palettes) {
			p.writeTo(os);
		}
		os.writeShort(palMappings.size());
		for(PalMapping p : palMappings ) {
			p.writeTo(os);
		}
		os.writeShort(frameSeqMap.size());
		for(FrameSeq fs:frameSeqMap.values()) {
			fs.writeTo(os);
		}
	}
}
