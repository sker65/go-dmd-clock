package com.rinke.solutions.pinball.view;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import com.rinke.solutions.beans.BeanFactory;
import com.rinke.solutions.pinball.GlobalExceptionHandler;

@Slf4j
public class ChangeHandlerHelper<T> {
	
	BeanFactory beanFactory;
	
	Class<T> clazz;

	public ChangeHandlerHelper(BeanFactory beanFactory, Class<T> clazz) {
		super();
		this.beanFactory = beanFactory;
		this.clazz = clazz;
	}

	private static class HandlerInvocation<T> {
		T handler;
		Method method;
		public HandlerInvocation(T handler, Method method) {
			super();
			this.handler = handler;
			this.method = method;
		}
		@Override
		public String toString() {
			return String.format("HandlerInvocation [handler=%s, method=%s]", handler.getClass().getSimpleName(), method.getName());
		}
	}
	
	Map<String,List<HandlerInvocation<T>>> invocationCache = new HashMap<>();

	public void callOnChangedHandlers(String propName, Object nv, Object ov) throws RuntimeException {
		if( invocationCache.containsKey(propName)) {
			for( HandlerInvocation<T> hi : invocationCache.get(propName)) {
				try {
					log.debug("{}.{}({},{})", 
							hi.handler.getClass().getSimpleName(), hi.method.getName(), ov, nv);
					hi.method.invoke(hi.handler, new Object[]{ov,nv});
				} catch (InvocationTargetException e) {
					handleInvocationException(hi, e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("error calling " + hi, e);
				}
			}
		} else {
			searchAndCallChangeHandlers(propName, nv, ov);
		}
	}

	private void handleInvocationException(HandlerInvocation<T> hi, InvocationTargetException e) {
		log.error("error calling {}", hi, e);
		GlobalExceptionHandler.getInstance().showError(e);
	}

	void searchAndCallChangeHandlers(String propName, Object nv, Object ov) {
		Class<?> clz = nv!=null?nv.getClass():(ov!=null?ov.getClass():null);
		Class<?> clz1 = null;
		List<T> bindingHandlers = beanFactory.getBeansOfType(clazz);
		String methodName = "on"+StringUtils.capitalize(propName)+"Changed";
		if( clz != null ) {
			clz1 = toPrimitive(clz);
			for(T h : bindingHandlers) {
				HandlerInvocation<T> hi = null;
				try {
					Method m = findMethod(methodName, h.getClass(), clz );
					hi = new HandlerInvocation<T>(h, m);
					log.debug("{}.{}({},{})", 
							h.getClass().getSimpleName(), m.getName(), ov, nv);
					m.invoke(h, new Object[]{ov,nv});
					addToCache(propName,m,h);
				} catch ( IllegalAccessException  e1) {
					log.error("error calling {}", methodName, e1);
					throw new RuntimeException("error calling "+methodName, e1);
				} catch (NoSuchMethodException e1) {
					// try to find method with a signature that uses primitive types
					if( clz1 != null ) {
						try {
							Method m = findMethod(methodName, h.getClass(), clz1 );
							hi = new HandlerInvocation<T>(h, m);
							m.invoke(h, new Object[]{ov,nv});
							addToCache(propName,m,h);
						} catch ( IllegalAccessException e2) {
							log.error("error calling {}", methodName, e2);
							throw new RuntimeException("error calling "+methodName, e1);
						} catch (NoSuchMethodException e2) {
						} catch (InvocationTargetException e) {
							handleInvocationException(hi, e);
						}				
					}
				} catch (InvocationTargetException e) {
					handleInvocationException(hi, e);
				}
			}
		}
		// if no method found add an empty list, to prevent repeated search
		if( !invocationCache.containsKey(propName) ) {
			invocationCache.put(propName, new ArrayList<>());
		}
	}
	
	private Method findMethod(String methodName, Class<?> handler, Class<?> clz) throws NoSuchMethodException {
		for(Method m : handler.getDeclaredMethods()) {
			if( methodName.equals(m.getName())) {
				if( m.getParameterCount()==2 ) {
					Class<?>[] parameterTypes = m.getParameterTypes();
					if( parameterTypes[0].isAssignableFrom(clz) && parameterTypes[1].isAssignableFrom(clz)) {
						return m;
					}
				}
			}
		}
		throw new NoSuchMethodException();
	}

	synchronized private void addToCache(String propName, Method m, T h) {
		List<HandlerInvocation<T>> list = invocationCache.get(propName);
		if( list == null ) {
			list = new ArrayList<>();
			invocationCache.put(propName, list);
		}
		HandlerInvocation<T> i = new HandlerInvocation<T>(h, m);
		list.add(i);
		log.debug("add invocation to cache {}", i);
	}

	private Class<?> toPrimitive(Class<?> clz) {
		if( Integer.class.equals(clz) ) {
			return Integer.TYPE;
		} else if( Boolean.class.equals(clz)) {
			return Boolean.TYPE;
		} else if( Long.class.equals(clz)) {
			return Long.TYPE;
		}
		return null;
	}


}
