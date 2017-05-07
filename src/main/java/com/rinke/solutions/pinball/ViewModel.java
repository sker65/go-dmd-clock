package com.rinke.solutions.pinball;

import com.rinke.solutions.beans.Bean;

@Bean
public class ViewModel {
	
	private String paletteName;

	public String getPaletteName() {
		return paletteName;
	}

	public void setPaletteName(String paletteName) {
		this.paletteName = paletteName;
	}

	@Override
	public String toString() {
		return String.format("ViewModel [paletteName=%s]", paletteName);
	}

}
