package com.rinke.solutions.pinball.ui;

import java.util.Map;

import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation.RecordingLink;
import com.rinke.solutions.pinball.view.View;

public interface EditLinkView extends View {

	public void setRecordingLink(RecordingLink rl);

	public void setSceneName(String name);

	public void setRecordings(Map<String, Animation> v);
	
	public RecordingLink getRecordingLink();
	
	public boolean okClicked();

}