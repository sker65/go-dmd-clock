package com.rinke.solutions.pinball.animation;

public interface ProgressEventListener {
	public void notify( ProgressEvent evt);
	public static class ProgressEvent {
		public final int progress;
		public final String job;
		public ProgressEvent(int progress, String job) {
			super();
			this.progress = progress;
			this.job = job;
		}
	}
}
