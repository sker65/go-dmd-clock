package com.rinke.solutions.pinball.view;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.view.handler.ViewHandler;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.view.model.XStreamUtil;

@Slf4j
@Bean
public class ReflectionDispatcher implements CmdDispatcher {
	
	@Autowired
	ViewModel viewModel;
	
	@Autowired
	XStreamUtil xStreamUtil;
	
	List<ViewHandler> handler = new ArrayList<>();

	public void registerHandler(ViewHandler o) {
		log.debug("registering handler: {}", o.getClass().getSimpleName());
		handler.add(o);
	}
	
	private static class HandlerInvocation {
		public Method m;
		public ViewHandler handler;
		public HandlerInvocation(Method m, ViewHandler handler) {
			super();
			this.m = m;
			this.handler = handler;
		}
		@Override
		public String toString() {
			return String.format("HandlerInvocation [m=%s, handler=%s]", m.getName(), handler.getClass().getSimpleName());
		}
	}
	
	Map<Command,List<HandlerInvocation>> invocationCache = new HashMap<>();
	
	@Override
	public <T> void dispatch(Command<T> cmd) {
		boolean wasHandled = false;
		List<HandlerInvocation> invocationList = invocationCache.get(cmd);
		if( invocationList != null ) {
			callCachedHandlers(cmd, invocationList);
			wasHandled = true;
		} else {
			String methodName = "on"+StringUtils.capitalize(cmd.name);
			wasHandled = scanForHandlers(cmd, methodName);
		}
		if( !wasHandled ) {
			log.error("**** cmd {} was not handled", cmd);
			throw new RuntimeException("cmd "+cmd.name+ " was not handled");
		}
		//log.info( xStreamUtil.toXML(viewModel) );
	}

	<T> boolean scanForHandlers(Command<T> cmd,  String methodName) {
		boolean wasHandled = false;
		for( ViewHandler handler : handler) {
			Method[] methods = handler.getClass().getDeclaredMethods();
			for( Method m : methods) {
				if( m.getName().equals(methodName) ) {
					try {
						if( cmd.param != null && m.getParameterCount() > 0) {
							m.invoke(handler, cmd.param);
							addToCache(m,handler,cmd);
							wasHandled = true;
							break;
						} else if( cmd.param == null && m.getParameterCount()==0) {
							m.invoke(handler);
							wasHandled = true;
							break;
						}
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.error("Error calling {}", m.getName(), unroll(e));
						throw new RuntimeException("error calling "+m.getName(), unroll(e));
					}
				}
			}
		}
		return wasHandled;
	}

	<T> void callCachedHandlers(Command<T> cmd, List<HandlerInvocation> invocationList) {
		for( HandlerInvocation hi : invocationList ) {
			try {
				if( cmd.param != null ) {
					hi.m.invoke(hi.handler, cmd.param);
				} else {
					hi.m.invoke(hi.handler);
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error calling {}", hi.m.getName(), unroll(e));
				throw new RuntimeException("error calling "+hi.m.getName(), unroll(e));
			}
		}
	}
	
	synchronized private <T> void addToCache(Method m, ViewHandler handler, Command<T> cmd) {
		List<HandlerInvocation> invocationList = invocationCache.get(cmd);
		if( invocationList == null ) {
			invocationList = new ArrayList<>();
			invocationCache.put(cmd, invocationList);
		}
		invocationList.add(new HandlerInvocation(m, handler));
	}

	private Throwable unroll(Throwable e) {
		return ( e instanceof InvocationTargetException ) ? e.getCause() : e;
	}

	@Override
	public List<ViewHandler> getViewHandlers() {
		return handler;
	}

}
