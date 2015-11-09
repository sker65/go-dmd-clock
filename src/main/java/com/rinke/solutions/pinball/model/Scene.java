package com.rinke.solutions.pinball.model;

public class Scene {
	String name;
	int start;
	int end;
	
	public Scene(String name, int start, int end) {
		super();
		this.name = name;
		this.start = start;
		this.end = end;
	}

	@Override
	public String toString() {
		return "Scene [name=" + name + ", start=" + start + ", end=" + end
				+ "]";
	}
	
	
	
}
