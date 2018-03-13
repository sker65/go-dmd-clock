package com.rinke.solutions.pinball.ui;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
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
		super(parent, style +(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL));
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
		//shell.setSize(450, 300);
		shell.setText(getText());
		
		shell.setLayout(new GridLayout(3, true));
		
		GridData data = new GridData();
        Label image = new Label(shell, SWT.NONE);
        image.setImage(shell.getDisplay().getSystemImage(this.iconNo));
        data = new GridData(SWT.LEFT, SWT.BEGINNING, true, false);
        image.setLayoutData(data);
        
        Label lblHeader = new Label(shell, SWT.NONE);
        //lblHeader.setFont(SWTResourceManager.getFont(".SF NS Text", 13, SWT.BOLD));
        lblHeader.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        lblHeader.setText(this.header);
        
        Label lblMessage = new Label(shell, SWT.NONE);
        lblMessage.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true, 3, 1));
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
				new CustomMessageBox(new Shell(), 0, SWT.ICON_ERROR,
						"Warning", "Unsaved changes", "There are unsaved changes in project",
				new String[]{"", "Cancel", "Proceed"}, 2);

        int r = dialog.open();
        System.out.println(r);
    }

}
