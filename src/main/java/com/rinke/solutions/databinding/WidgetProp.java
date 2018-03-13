package com.rinke.solutions.databinding;

public enum WidgetProp {
	NONE("",""), ENABLED("","Enabled"), LABEL("","Label"), TEXT("","Text"), 
	SINGLE_SELECTION("selected", ""), SELECTION("selected", ""), INPUT("",""), MIN("min",""), MAX("max","");
	
	private WidgetProp(String prefix, String postfix) {
		this.postfix = postfix;
		this.prefix = prefix;
	}
	public String postfix;
	public String prefix;
}