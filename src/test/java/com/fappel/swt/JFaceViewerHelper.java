package com.fappel.swt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JFaceViewerHelper {
	
	private static Logger LOG = LoggerFactory.getLogger(JFaceViewerHelper.class);

	public static void fireSelectionChanged(Viewer src, SelectionChangedEvent e) {
		try {
			Method method = Viewer.class.getDeclaredMethod("fireSelectionChanged", SelectionChangedEvent.class);
			method.setAccessible(true);
			method.invoke(src, e);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
			LOG.error("problems calling fireSelectionChanged", e1);
		}
	}

}
