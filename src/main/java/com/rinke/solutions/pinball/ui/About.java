package com.rinke.solutions.pinball.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class About extends Dialog {

	protected Shell shlAboutPindmdEditor;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public About(Shell parent) {
		super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
		setText("About pin2dmd editor");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public void open() {
		createContents();
		shlAboutPindmdEditor.open();
		shlAboutPindmdEditor.layout();
		Display display = getParent().getDisplay();
		while (!shlAboutPindmdEditor.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
	    
	    
		shlAboutPindmdEditor = new Shell(getParent(), getStyle());
		shlAboutPindmdEditor.setSize(385, 237);
		shlAboutPindmdEditor.setText("About pin2dmd editor");

		ResourceManager resManager = 
                new LocalResourceManager(JFaceResources.getResources(),shlAboutPindmdEditor);
		
		Label logo = new Label(shlAboutPindmdEditor, SWT.NONE);
		logo.setImage(resManager.createImage(ImageDescriptor.createFromFile(About.class, "/logo.png")));
		logo.setBounds(10, 10, 195, 114);
		
		Button btnOk = new Button(shlAboutPindmdEditor, SWT.NONE);
		btnOk.addListener(SWT.Selection, e->shlAboutPindmdEditor.close());
		btnOk.setBounds(147, 177, 94, 28);
		btnOk.setText("OK");
		
		Label lblBySteve = new Label(shlAboutPindmdEditor, SWT.NONE);
		lblBySteve.setBounds(211, 22, 144, 103);
		lblBySteve.setText("by Steve\n(C) 2015\n\n\nhttp://github.com/\nsker65/go-dmd-clock");


	}
}
