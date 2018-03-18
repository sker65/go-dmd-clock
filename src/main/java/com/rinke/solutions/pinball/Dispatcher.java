package com.rinke.solutions.pinball;

public interface Dispatcher {

	public void asyncExec(Runnable runnable);

	public void timerExec(int milliseconds, Runnable runnable);

	public void syncExec(Runnable runnable);

}