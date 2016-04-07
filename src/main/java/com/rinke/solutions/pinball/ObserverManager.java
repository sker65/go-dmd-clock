package com.rinke.solutions.pinball;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Observable;
import java.util.Observer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObserverManager {
	
	private static final Logger LOG = LoggerFactory.getLogger(ObserverManager.class);

	public static interface BooleanConsumer extends Consumer<Boolean>{

	}

	public static Observer bind(Observable observable, BooleanConsumer booleanConsumer, 
			BooleanSupplier booleanSupplier) {
		
		Observer observer = new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				//LOG.info("update from {}", o);
				boolean b = booleanSupplier.getAsBoolean();
				booleanConsumer.accept(b);
			}
			
		};
		observable.addObserver(observer);
		
		// force calling
		try {
            Method method = Observable.class.getDeclaredMethod("setChanged");
            method.setAccessible(true);
            method.invoke(observable);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            LOG.error("problem calling setChanged",e);
        }
		
		observable.notifyObservers();
		return observer;
	}

}
