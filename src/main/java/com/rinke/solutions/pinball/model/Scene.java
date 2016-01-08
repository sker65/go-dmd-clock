package com.rinke.solutions.pinball.model;

public class Scene {
	public String name;
	public int start;
	public int end;
	public int palIndex;
	
	public Scene(String name, int start, int end, int palIndex) {
		super();
		this.name = name;
		this.start = start;
		this.end = end;
		this.palIndex = palIndex;
	}

	@Override
	public String toString() {
		return "Scene [name=" + name + ", start=" + start + ", end=" + end
				+ ", palIndex=" + palIndex + "]";
	}
	
	
	
}
