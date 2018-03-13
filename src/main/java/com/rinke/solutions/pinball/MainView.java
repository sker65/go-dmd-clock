package com.rinke.solutions.pinball;

import com.rinke.solutions.beans.BeanFactory;
import com.rinke.solutions.pinball.view.model.ViewModel;


public interface MainView {
	
	public void playFullScreen();
	public void open();
	public void timerExec( int millis, Runnable r);
	public void createBindings();
	public void init(ViewModel vm, BeanFactory b);

}
