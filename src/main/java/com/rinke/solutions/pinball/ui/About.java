package com.rinke.solutions.pinball.ui;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class About extends Dialog {

	protected Object result;
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
	public Object open() {
		createContents();
		shlAboutPindmdEditor.open();
		shlAboutPindmdEditor.layout();
		Display display = getParent().getDisplay();
		while (!shlAboutPindmdEditor.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shlAboutPindmdEditor = new Shell(getParent(), getStyle());
		shlAboutPindmdEditor.setSize(385, 237);
		shlAboutPindmdEditor.setText("About pin2dmd editor");
		
		Label logo = new Label(shlAboutPindmdEditor, SWT.NONE);
		logo.setImage(SWTResourceManager.getImage(About.class, "/logo.png"));
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
