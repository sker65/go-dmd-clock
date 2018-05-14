package com.rinke.solutions.pinball.ui;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.slf4j.helpers.FormattingTuple;
import org.eclipse.wb.swt.SWTResourceManager;

public class CustomMessageBox extends Dialog {

	protected int result;
	protected Shell shell;
	private String header = "Header";
	private String message = "Message";
	private String[] buttons = { "", "Cancel", "Ok" };
	private int defaultBtn;
	private int iconNo = SWT.ICON_WARNING;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public CustomMessageBox(Shell parent, int style, int icon, String title, String header, 
			String message, String[] buttons, int defaultBtn) {
		super(parent, style | (SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL));
		setText(title);
		this.iconNo = icon;
		this.header = header;
		this.message = message;
		this.buttons = buttons;
		this.defaultBtn = defaultBtn;
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public int open() {
		createContents();
		shell.open();
		shell.pack();
		// center
		Rectangle parentSize = getParent().getBounds();
		Rectangle shellSize = shell.getBounds();
		int locationX = (parentSize.width - shellSize.width)/2+parentSize.x;
		int locationY = (parentSize.height - shellSize.height)/2+parentSize.y;
		shell.setLocation(new Point(locationX, locationY));
		
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
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
		shell = new Shell(getParent(), getStyle());
		shell.setMinimumSize(new Point(69, 12));
		shell.setSize(260, 118);
		//shell.setSize(450, 300);
		shell.setText(getText());
		
		GridLayout gl_shell = new GridLayout(3, true);
		gl_shell.marginTop = 5;
		gl_shell.marginRight = 5;
		gl_shell.marginLeft = 5;
		gl_shell.marginBottom = 15;
		shell.setLayout(gl_shell);
		
		GridData data = new GridData();
        Label image = new Label(shell, SWT.NONE);
        image.setImage(shell.getDisplay().getSystemImage(this.iconNo));
        data = new GridData(SWT.LEFT, SWT.BEGINNING, true, false);
        image.setLayoutData(data);
        
        Label lblHeader = new Label(shell, SWT.NONE);
        lblHeader.setFont(SWTResourceManager.getBoldFont(lblHeader.getFont()));
        lblHeader.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        lblHeader.setText(this.header);
        
        Label lblMessage = new Label(shell, SWT.WRAP);
        lblMessage.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true, 3, 1));
        lblMessage.setText(this.message);
        
        for(int i = 0; i<3 && i < buttons.length; i++) {
        	if( !StringUtils.isEmpty(buttons[i])) {
                Button btn = new Button(shell, SWT.NONE);
                btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
                btn.setText(buttons[i]);
                btn.setData(i);
                btn.addListener(SWT.Selection, e->btnPressed((Integer)btn.getData()));
                if( i == defaultBtn ) shell.setDefaultButton(btn);
        	} else {
        		new Label(shell, SWT.NONE);
        	}
        }
	
	}

	private void btnPressed(int data) {
		result = data;
		shell.close();
	}
	
	public static void main(String[] args)
    {
		CustomMessageBox dialog = 
//				new CustomMessageBox(new Shell(), 0, SWT.ICON_ERROR,
//						"Warning", "Unsaved changes", "There are unsaved changes in project",
//				new String[]{"", "Cancel", "Proceed"}, 2);
		
		new CustomMessageBox(new Shell(), 0, SWT.ICON_ERROR, "Warning",
				"Changing edit mode", 
				"you are about to change edit mode to while scene was already modified.",
				new String[]{"", "Cancel", "Change Mode"},2);
		
        int r = dialog.open();
        System.out.println(r);
    }

}
