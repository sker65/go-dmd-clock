package com.rinke.solutions.pinball.view.swt;

import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.rinke.solutions.pinball.view.model.TypedLabel;

public class IconMapLabelProvider extends ObservableMapLabelProvider {

	private LocalResourceManager resManager;
	
	public IconMapLabelProvider(Composite parent, IObservableMap[] observeMap) {
		super(observeMap);
		resManager = new LocalResourceManager(JFaceResources.getResources(),
				parent);	
	}

    public String getText(Object o) {
    	TypedLabel tl = (TypedLabel) o;
        return tl.label;
    }

	public Image getImage(Object o) {
		TypedLabel tl = (TypedLabel) o;
		String icon = "/icons/"+tl.type+".png";
		return resManager.createImage(
				ImageDescriptor.createFromFile(IconMapLabelProvider.class, icon));
	}

	@Override
	public Image getColumnImage(Object o, int columnIndex) {
		return getImage(o);
	}

	@Override
	public String getColumnText(Object o, int columnIndex) {
		return getText(o);
	}
	
}
