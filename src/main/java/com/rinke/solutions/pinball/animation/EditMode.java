package com.rinke.solutions.pinball.animation;

public enum EditMode {
	REPLACE("Replace"), COLMASK("Color Mask"), FIXED("Fixed"), FOLLOW("Color Mask Seq.");

	public final String label;
	
	private EditMode(String label) {
		this.label = label;
	}

	public static EditMode fromOrdinal(byte emo) {
		for (EditMode em : values()) {
			if( em.ordinal() == emo ) return em;
		}
		return null;
	}

	public String getLabel() {
		return label;
	}

}