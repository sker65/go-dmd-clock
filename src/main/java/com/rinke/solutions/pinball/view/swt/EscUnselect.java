package com.rinke.solutions.pinball.view.swt;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

public class EscUnselect implements KeyListener{
	
	private TableViewer viewer;

	public EscUnselect(TableViewer viewer) {
		super();
		this.viewer = viewer;
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		if(e.keyCode == SWT.ESC) viewer.setSelection(StructuredSelection.EMPTY);
	}

}
