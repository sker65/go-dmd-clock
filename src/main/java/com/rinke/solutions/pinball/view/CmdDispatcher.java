package com.rinke.solutions.pinball.view;

import java.util.List;

import com.rinke.solutions.pinball.view.handler.ViewHandler;

public interface CmdDispatcher {
	
	public static class Command<T> {
		public final String name;
		public final T param;
		
		public Command(T param, String name) {
			super();
			this.param = param;
			this.name = name;
		}

		@Override
		public String toString() {
			return String.format("Command [name=%s, param=%s]", name, param);
		}

	}
	
	public <T> void dispatch( Command<T> cmd );

	public void registerHandler(ViewHandler o);
	
	public List<ViewHandler> getViewHandlers();
}
