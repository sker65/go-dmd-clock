package com.rinke.solutions.pinball;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public class IconLabelProvider extends OwnerDrawLabelProvider {

    private LocalResourceManager resManager;
	private IconResolver resolver;
    
    @FunctionalInterface
    public interface IconResolver {
    	public Pair<String,String> resolve(Object o);
    }

	public IconLabelProvider(Composite parent, IconResolver resolver) {
		super();
		this.resolver = resolver;
		resManager = new LocalResourceManager(JFaceResources.getResources(),
				parent);	}

    public String getText(Object o) {
        return resolver.resolve(o).getRight();
    }

	public Image getImage(Object o) {
		String icon = "/icons/"+resolver.resolve(o).getLeft()+".png";
		return resManager.createImage(
				ImageDescriptor.createFromFile(IconLabelProvider.class, icon));
	}

	@Override
	protected void measure(Event event, Object element) {
		Rectangle rectangle = new Rectangle(0,0,16,16);
        if( event != null && element != null) {
    		event.setBounds(new Rectangle(event.x, event.y, rectangle.width + 200 , 
    	            rectangle.height));		
        }
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
