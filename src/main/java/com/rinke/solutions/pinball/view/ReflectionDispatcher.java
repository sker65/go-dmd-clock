package com.rinke.solutions.pinball.view;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
	
	@Override
	public <T> void dispatch(Command<T> cmd) {
		boolean wasHandled = false;
		String methodName = "on"+StringUtils.capitalize(cmd.name);
		for( ViewHandler handler : handler) {
			Method[] methods = handler.getClass().getDeclaredMethods();
			for( Method m : methods) {
				if( m.getName().equals(methodName) ) {
					try {
						if( cmd.param != null || m.getParameterCount() > 0) m.invoke(handler, cmd.param);
						else m.invoke(handler);
						wasHandled = true;
						break;
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.error("Error calling {}", m.getName(), unroll(e));
						throw new RuntimeException("error calling "+m.getName(), unroll(e));
					}
				}
			}
		}
		if( !wasHandled ) {
			log.error("**** cmd {} was not handled", cmd);
			//throw new RuntimeException("cmd "+cmd.name+ " was not handled");
		}
		
		//log.info( xStreamUtil.toXML(viewModel) );
	}

	private Throwable unroll(Throwable e) {
		return ( e instanceof InvocationTargetException ) ? e.getCause() : e;
	}

	@Override
	public List<ViewHandler> getViewHandlers() {
		return handler;
	}

}
