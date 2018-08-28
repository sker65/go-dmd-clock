package com.rinke.solutions.pinball;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.pinball.animation.ProgressEventListener;
import com.rinke.solutions.pinball.animation.ProgressEventListener.ProgressEvent;

@Slf4j
public abstract class Worker implements Runnable {
	
	protected boolean cancelRequested;
	private ProgressEventListener progressEvt;
	private long lastUpdate;
	private long interval = 300;
	@Getter
	private RuntimeException runtimeException = null;
	
	public void run() {
		try {
			innerRun();
		} catch(RuntimeException e) {
			runtimeException = e;
		}
	}
	
	public boolean hasError() {
		return runtimeException != null;
	}

	protected abstract void innerRun();

	public void notify(int progress, String job) {
		if( progressEvt != null ) {
			if( System.currentTimeMillis() - lastUpdate > interval ) {
				log.info("notify progress {}/{}", progress,job);
				progressEvt.notify(new ProgressEvent(progress, job));
				lastUpdate = System.currentTimeMillis();
				Thread.yield();
			}
		}
	}

	public void requestCancel() {
		this.cancelRequested = true;
	}

	public void setProgressEvt(ProgressEventListener progressEvt) {
		this.progressEvt = progressEvt;
	}

	public void setInterval(long interval) {
		 this.interval = interval;
	}
}