package com.rinke.solutions.pinball.ui;

import com.rinke.solutions.pinball.Worker;
import com.rinke.solutions.pinball.animation.ProgressEventListener;

public interface IProgress extends ProgressEventListener{
	public void open(Worker w);
	public void setText(String string);
}
