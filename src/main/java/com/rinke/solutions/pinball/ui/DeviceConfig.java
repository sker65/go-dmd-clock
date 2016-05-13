package com.rinke.solutions.pinball.ui;

import java.io.FileOutputStream;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.io.ConnectorFactory;
import com.rinke.solutions.pinball.io.IpConnector;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.ConnectionHandle;
import com.rinke.solutions.pinball.model.DefaultPalette;
import com.rinke.solutions.pinball.model.DeviceMode;

import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

@Slf4j
public class DeviceConfig extends Dialog {
    
    protected Object result;
    protected Shell shell;
    private String lastPath;
    private ComboViewer deviceModecomboViewer;
    private ComboViewer comboViewerDefaultPalette;
    private Combo deviceModeCombo;
    private Combo comboDefaultPalette;
    private Text pin2dmdHost;
    
    public String getPin2DmdHost() {
    	return address;
    }

    /**
     * Create the dialog.
     * @param parent
     * @param style
     */
    public DeviceConfig(Shell parent) {
        super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
        setText("Device Config");
    }

    // testability overridden by tests
    protected FileChooser createFileChooser(Shell shell, int flags) {   
        return new FileDialogDelegate(shell, flags);
    }
    
    protected void save() {
        FileChooser fileChooser = createFileChooser(shell, SWT.SAVE);
        fileChooser.setOverwrite(true);
        fileChooser.setFileName("pin2dmd.dat");
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.dat" });
        fileChooser.setFilterNames(new String[] {  "pin2dmd dat" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();        
        if (filename != null) {
            writeDeviceConfig(filename, deviceModeCombo.getSelectionIndex(), comboDefaultPalette.getSelectionIndex());
        }
        shell.close();
    }

    void writeDeviceConfig(String filename, int mode, int defPalette) {
        byte[] dummy = {0x0F,0x0A,0x0F,0x0C,0x0F,0x00,0x0F,0x0F,0,0};
        try(FileOutputStream 
            fos = new FileOutputStream(filename) ) {
            fos.write(mode);
            fos.write(defPalette);
            fos.write(dummy);
        } catch(IOException e) {
            log.error("problems writing {}", filename,e);
            throw new RuntimeException("error writing "+filename, e);
        }
    }
    
    private String address;

    /**
     * Open the dialog.
     * @return the result
     */
    public Object open(String address) {
        createContents();
        this.address = address;
        
        pin2dmdHost.setText(address!=null?address:"");
        
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
    void createContents() {
        shell = new Shell(getParent(), getStyle());
        shell.setSize(480, 231);
        shell.setText("Device Configuration");
        shell.setLayout(new FormLayout());
        
        Group grpConfig = new Group(shell, SWT.NONE);
        FormData fd_grpConfig = new FormData();
        fd_grpConfig.top = new FormAttachment(0, 10);
        fd_grpConfig.left = new FormAttachment(0, 10);
        fd_grpConfig.right = new FormAttachment(0, 470);
        grpConfig.setLayoutData(fd_grpConfig);
        grpConfig.setText("Config");
        grpConfig.setLayout(new GridLayout(3, false));
        
        Label lblNewLabel = new Label(grpConfig, SWT.NONE);
        lblNewLabel.setText("Device Mode");
        
        deviceModecomboViewer = new ComboViewer(grpConfig, SWT.NONE);
        deviceModeCombo = deviceModecomboViewer.getCombo();
        deviceModeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        deviceModecomboViewer.setContentProvider(ArrayContentProvider.getInstance());
        deviceModecomboViewer.setInput(DeviceMode.values());
        deviceModeCombo.select(0);
        
        Button btnSave = new Button(grpConfig, SWT.NONE);
        btnSave.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnSave.setText("Save");
        btnSave.addListener(SWT.Selection, e->save());
        
        Label lblNewLabel_1 = new Label(grpConfig, SWT.NONE);
        lblNewLabel_1.setText("Default Palette");
        
        comboViewerDefaultPalette = new ComboViewer(grpConfig, SWT.NONE);
        comboDefaultPalette = comboViewerDefaultPalette.getCombo();
        GridData gd_comboDefaultPalette = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gd_comboDefaultPalette.widthHint = 245;
        comboDefaultPalette.setLayoutData(gd_comboDefaultPalette);
        comboViewerDefaultPalette.setContentProvider(ArrayContentProvider.getInstance());
        comboViewerDefaultPalette.setInput(DefaultPalette.values());
        comboDefaultPalette.select(0);
        
        Button btnCancel = new Button(grpConfig, SWT.NONE);
        btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnCancel.setText("Cancel");
        btnCancel.addListener(SWT.Selection, e->shell.close());
        
        Group grpWifi = new Group(shell, SWT.NONE);
        fd_grpConfig.bottom = new FormAttachment(grpWifi, -6);
        grpWifi.setText("WiFi");
        grpWifi.setLayout(new GridLayout(3, false));
        FormData fd_grpWifi = new FormData();
        fd_grpWifi.bottom = new FormAttachment(100, -10);
        fd_grpWifi.top = new FormAttachment(0, 114);
        fd_grpWifi.left = new FormAttachment(0, 10);
        fd_grpWifi.right = new FormAttachment(100, -8);
        grpWifi.setLayoutData(fd_grpWifi);
        
        Label lblAdress = new Label(grpWifi, SWT.NONE);
        GridData gd_lblAdress = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAdress.widthHint = 83;
        lblAdress.setLayoutData(gd_lblAdress);
        lblAdress.setText("Adress");
        
        pin2dmdHost = new Text(grpWifi, SWT.BORDER);
        GridData gd_text = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_text.widthHint = 267;
        pin2dmdHost.setLayoutData(gd_text);
        
        Button btnConnectBtn = new Button(grpWifi, SWT.NONE);
        btnConnectBtn.addListener(SWT.Selection, e->testConnect(pin2dmdHost.getText()));
        btnConnectBtn.setText("Connect");
        
        Label lblEnterIpAddress = new Label(grpWifi, SWT.NONE);
        lblEnterIpAddress.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        lblEnterIpAddress.setText("Enter IP address or hostname for WiFi (default port is "
        		+IpConnector.DEFAULT_PORT+")");
        new Label(grpWifi, SWT.NONE);
        new Label(grpWifi, SWT.NONE);
    }

	private void testConnect(String address) {
		Pin2DmdConnector connector = ConnectorFactory.create(address);
		ConnectionHandle handle = connector.connect(address);
		connector.release(handle);
		this.address = address;
	}
}
