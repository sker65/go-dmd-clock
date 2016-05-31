package com.rinke.solutions.pinball;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import com.rinke.solutions.pinball.model.PalMapping;

public class KeyframeLabelProvider extends OwnerDrawLabelProvider {

    private LocalResourceManager resManager;

	public KeyframeLabelProvider(Composite parent) {
		super();
		resManager = new LocalResourceManager(JFaceResources.getResources(),
				parent);	}

    public String getText(Object o) {
        return ((PalMapping)o).name;
    }

	public Image getImage(Object o) {
		PalMapping p = (PalMapping)o;
		String icon = "/icons/"+p.switchMode.name().toLowerCase()+".png";
		return resManager.createImage(
				ImageDescriptor.createFromFile(KeyframeLabelProvider.class, icon));
	}

	@Override
	protected void measure(Event event, Object element) {
		Rectangle rectangle = new Rectangle(0,0,16,16);
        if( event != null && element != null)
		event.setBounds(new Rectangle(event.x, event.y, rectangle.width + 200 , 
            rectangle.height));		
	}

	@Override
	protected void paint(Event event, Object item) {
        Rectangle bounds = event.getBounds();
        String text = getText(item);
        //Point point = event.gc.stringExtent(text);
        event.gc.drawImage(getImage(item), bounds.x + 2, bounds.y+2);
        event.gc.drawText(text, bounds.x+20, bounds.y, true);
		
	}


}
