package com.rinke.solutions.pinball.view.handler;


public interface ViewBindingHandler {
	public default void viewModelChanged(String propName, Object ov, Object nv) {};
}
