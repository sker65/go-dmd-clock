package com.rinke.solutions.pinball.view;

import java.util.List;

import com.rinke.solutions.pinball.view.handler.CommandHandler;

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
		
		// hash and equals only check if a param is set but not which

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((param == null) ? 0 : 25);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Command other = (Command) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (param == null) {
				if (other.param != null)
					return false;
			} 
			return true;
		}

	}
	
	public <T> void dispatch( Command<T> cmd );

	public void registerHandler(CommandHandler o);
	
	public List<CommandHandler> getCommandHandlers();
	
}