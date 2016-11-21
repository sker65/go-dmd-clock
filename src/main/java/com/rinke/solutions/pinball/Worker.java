package com.rinke.solutions.pinball;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.pinball.animation.ProgressEventListener;
import com.rinke.solutions.pinball.animation.ProgressEventListener.ProgressEvent;

@Slf4j
public abstract class Worker implements Runnable {
	
	protected boolean cancelRequested;
	private ProgressEventListener progressEvt;
	private long lastUpdate;

	public Worker(ProgressEventListener progressEvt) {
		super();
		this.progressEvt = progressEvt;
	}
	
	public void notify(int progress, String job) {
		if( progressEvt != null ) {
			if( System.currentTimeMillis() - lastUpdate > 500 ) {
				log.info("notify progress {}/{}", progress,job);
				progressEvt.notify(new ProgressEvent(progress, job));
				lastUpdate = System.currentTimeMillis();
			}
		}
	}

	public void requestCancel() {
		this.cancelRequested = true;
	}
}