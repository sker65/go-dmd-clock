package com.rinke.solutions.pinball.widget;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public abstract class ResourceManagedCanvas extends Canvas {

	protected ResourceManager resourceManager;

	public ResourceManagedCanvas(Composite parent, int style) {
		super(parent, style);
		// The LocalResourceManager attaches the DisposeHandler to the Canvas for you
		resourceManager = new LocalResourceManager( JFaceResources.getResources(), this);
	    addPaintListener(e->paintWidget(e));
	}

	protected abstract void paintWidget(PaintEvent e);

	@Override
	public abstract Point computeSize(int wHint, int hHint, boolean changed);
	
}