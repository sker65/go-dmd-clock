package com.rinke.solutions.pinball.ui;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

import com.rinke.solutions.pinball.DeviceMode;
import com.rinke.solutions.pinball.io.UsbTool;
import com.rinke.solutions.pinball.model.DefaultPalette;

public class UsbConfig extends Dialog {

	protected Object result;
	protected Shell shell;
	UsbTool usbTool = new UsbTool();

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public UsbConfig(Shell parent) {
		super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
		setText("SWT Dialog");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		shell.open();
		shell.layout();
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
		shell.setSize(450, 300);
		shell.setText("Usb Config");
		shell.setLayout(new GridLayout(3, false));
		
		Label lblDeviceMode = new Label(shell, SWT.NONE);
		lblDeviceMode.setText("Device Mode:");
		
		Combo deviceModeCombo = new Combo(shell, SWT.NONE);
		GridData gd_combo = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_combo.widthHint = 165;
		deviceModeCombo.setLayoutData(gd_combo);
		for(DeviceMode mode : DeviceMode.values()) {
			deviceModeCombo.add(mode.name(), mode.ordinal());
		}
		
		Button btnSetDeviceMode = new Button(shell, SWT.NONE);
		btnSetDeviceMode.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnSetDeviceMode.setText("Set Device Mode");
		btnSetDeviceMode.addListener(SWT.Selection, e->usbTool.switchToMode(deviceModeCombo.getSelectionIndex()));
		
		Label lblPalette = new Label(shell, SWT.NONE);
		lblPalette.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPalette.setText("Palette:");
		
		ComboViewer comboViewerDefaultPalette = new ComboViewer(shell, SWT.NONE);
		Combo comboDefaultPalette = comboViewerDefaultPalette.getCombo();
        GridData gd_comboDefaultPalette = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gd_comboDefaultPalette.widthHint = 161;
        comboDefaultPalette.setLayoutData(gd_comboDefaultPalette);
        comboViewerDefaultPalette.setContentProvider(ArrayContentProvider.getInstance());
        comboViewerDefaultPalette.setInput(DefaultPalette.values());
        comboDefaultPalette.select(0);
		
		Button btnSetPalette = new Button(shell, SWT.NONE);
		btnSetPalette.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnSetPalette.setText("Set Palette");
		btnSetPalette.addListener(SWT.Selection, e->usbTool.switchToPal(comboDefaultPalette.getSelectionIndex()));

		new Label(shell, SWT.NONE);
		
		Button btnResetDevice = new Button(shell, SWT.NONE);
		btnResetDevice.setText("Reset Device");
		btnResetDevice.addListener(SWT.Selection, e->usbTool.sendReset() );
		
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		
		Button btnOk = new Button(shell, SWT.NONE);
		btnOk.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnOk.setText("Ok");
		btnOk.addListener(SWT.Selection, e->shell.close());

	}
}
