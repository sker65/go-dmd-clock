package com.rinke.solutions.pinball.ui;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
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
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Scope;
import com.rinke.solutions.pinball.model.DefaultPalette;
//import com.rinke.solutions.pinball.model.DeviceMode;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.View;

import com.rinke.solutions.pinball.io.ConnectorFactory;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.UsbCmd;
import com.rinke.solutions.pinball.io.UsbConnector;

@Slf4j
@Bean(name="deviceConfig", scope=Scope.PROTOTYPE)

public class DeviceConfig extends Dialog implements View {

	public enum DeviceMode {
	    PinMame, WPC, Stern, WhiteStar, Spike, DataEast, Gottlieb1, Gottlieb2, Gottlieb3, Capcom, AlvinG, Spooky, DE128x16, Inder, Sleic, HomePin 
	}

	protected Shell shell;
    private String lastPath;
    private ComboViewer deviceModecomboViewer;
    private ComboViewer comboViewerDefaultPalette;
    private Combo deviceModeCombo;
    private Combo comboDefaultPalette;
    
	@Autowired MessageUtil messageUtil;
    
    Pin2DmdConnector connector = ConnectorFactory.create(null);
    
   /**
     * Create the dialog.
     * @param parent
     * @param style
     */
    public DeviceConfig(Shell parent) {
        super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
        setText("Configuration");
    }

    // testability overridden by tests
    protected FileChooser createFileChooser(Shell shell, int flags) {   
        return new FileDialogDelegate(shell, flags);
    }
    
    protected void loadUSB() {
        byte[] config = connector.loadConfig();
        if (config != null) {
	        deviceModeCombo.select(config[0]);
	        comboDefaultPalette.select(config[3]);
	        btnInvertClock.setEnabled(config[2]!=0);
	        scBrightness.setSelection(config[1]);
        } else {
			messageUtil.warn("PIN2DMD device not found","Please check USB connection");
        }
    }
    
    protected void load() {
        FileChooser fileChooser = createFileChooser(shell, SWT.OPEN);
        fileChooser.setFileName("pin2dmd.dat");
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.dat" });
        fileChooser.setFilterNames(new String[] {  "pin2dmd dat" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();        
        if (filename != null) {
        	try(InputStream in = new FileInputStream(filename)) {
            	byte[] config = IOUtils.toByteArray(in);
        		deviceModeCombo.select(config[0x00]);
    	        comboDefaultPalette.select(config[0x01]);
    	        btnInvertClock.setSelection(config[0x15]!=0);
    	        scBrightness.setSelection(config[0x16]);
            } catch(IOException e) {
                log.error("problems writing {}", filename,e);
                throw new RuntimeException("error writing "+filename, e);
            }
        }
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
            writeDeviceConfig(filename, deviceModeCombo.getSelectionIndex(), comboDefaultPalette.getSelectionIndex(),
            		btnInvertClock.getSelection(), scBrightness.getSelection(), 0/*rgbSeqCombo.getSelectionIndex()*/);
        }
    }

    void writeDeviceConfig(String filename, int mode, int defPalette, boolean invertClock, int brightness, int rgbSeq) {
        byte[] smartdmd = {0x0F,0x0A,0x0F,0x0C,0x0F,0x00,0x0F,0x0F}; // smartdmd sig
        byte[] foo = new byte[10];
        
        try(FileOutputStream 
            fos = new FileOutputStream(filename) ) {
            fos.write(mode);
            fos.write(defPalette);
            fos.write(smartdmd);
            fos.write(foo);
            fos.write(rgbSeq);
            fos.write(invertClock?1:0);
            fos.write(brightness);
        } catch(IOException e) {
            log.error("problems writing {}", filename,e);
            throw new RuntimeException("error writing "+filename, e);
        }
    }
    
	private Button btnInvertClock;
	private Scale scBrightness;
	private Group grpConfig;
	private FormData fd_grpConfig;

    /**
     * Open the dialog.
     * @return the result
     */
    public void open() {
        createContents();
        
        shell.open();
        shell.layout();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Create contents of the dialog.
     */
    void createContents() {
        shell = new Shell(getParent(), getStyle());
        shell.setSize(480, 262);
        shell.setText("Device Configuration");
        shell.setLayout(new FormLayout());
        
        grpConfig = new Group(shell, SWT.NONE);
        fd_grpConfig = new FormData();
        fd_grpConfig.top = new FormAttachment(0, 10);
        fd_grpConfig.left = new FormAttachment(0, 10);
        fd_grpConfig.right = new FormAttachment(0, 470);
        grpConfig.setLayoutData(fd_grpConfig);
        grpConfig.setText("Config");
        grpConfig.setLayout(new GridLayout(7, false));
        
        Label lblNewLabel = new Label(grpConfig, SWT.NONE);
        lblNewLabel.setText("Device Mode");
        
        deviceModecomboViewer = new ComboViewer(grpConfig, SWT.NONE);
        deviceModeCombo = deviceModecomboViewer.getCombo();
        deviceModeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1));
        deviceModecomboViewer.setContentProvider(ArrayContentProvider.getInstance());
        deviceModecomboViewer.setInput(DeviceMode.values());
        deviceModeCombo.select(0);
        deviceModeCombo.addListener(SWT.Selection, e->connector.switchToMode(deviceModeCombo.getSelectionIndex(),null));
        
        Label lblNewLabel_1 = new Label(grpConfig, SWT.NONE);
        lblNewLabel_1.setText("Default Palette");
        
        comboViewerDefaultPalette = new ComboViewer(grpConfig, SWT.NONE);
        comboDefaultPalette = comboViewerDefaultPalette.getCombo();
        GridData gd_comboDefaultPalette = new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1);
        gd_comboDefaultPalette.widthHint = 263;
        comboDefaultPalette.setLayoutData(gd_comboDefaultPalette);
        comboViewerDefaultPalette.setContentProvider(ArrayContentProvider.getInstance());
        comboViewerDefaultPalette.setInput(DefaultPalette.values());
        comboDefaultPalette.select(0);
        comboDefaultPalette.addListener(SWT.Selection, e->connector.switchToPal(comboDefaultPalette.getSelectionIndex(), null));
        
        Label lblBrightness = new Label(grpConfig, SWT.NONE);
        lblBrightness.setText("Brightness");
        
        scBrightness = new Scale(grpConfig, SWT.NONE);
        GridData gd_scale = new GridData(SWT.FILL, SWT.CENTER, false, false, 6, 1);
        gd_scale.heightHint = 43;
        scBrightness.setLayoutData(gd_scale);
        scBrightness.setMinimum(0);
        scBrightness.setMaximum(255);
        scBrightness.setIncrement(1);
        scBrightness.addListener(SWT.Selection, e->connector.sendBrightness(scBrightness.getSelection()));
        
        Label lblInvertClock = new Label(grpConfig, SWT.NONE);
        lblInvertClock.setText("Enhancer");
        
        btnInvertClock = new Button(grpConfig, SWT.CHECK);
        btnInvertClock.setSelection(true);
        GridData gd_btnInvertClock = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_btnInvertClock.widthHint = 18;
        btnInvertClock.setLayoutData(gd_btnInvertClock);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        new Label(grpConfig, SWT.NONE);
        
        Button btnLoadUSB = new Button(grpConfig, SWT.NONE);
        btnLoadUSB.setText("Load USB");
        btnLoadUSB.addListener(SWT.Selection, e->loadUSB() );
        
                
        Button btnSaveUSB = new Button(grpConfig, SWT.NONE);
        btnSaveUSB.setText("Save USB");
        btnSaveUSB.addListener(SWT.Selection, e->connector.sendCmd(UsbCmd.SAVE_CONFIG) );
        new Label(grpConfig, SWT.NONE);
        
        Button btnLoad = new Button(grpConfig, SWT.NONE);
        btnLoad.setText("Load File");
        btnLoad.addListener(SWT.Selection, e->load());
        
        Button btnSave = new Button(grpConfig, SWT.NONE);
        btnSave.setText("Save File");
        btnSave.addListener(SWT.Selection, e->save());
        
        Button btnOk = new Button(grpConfig, SWT.NONE);
        GridData gd_btnOk = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_btnOk.widthHint = 52;
        btnOk.setLayoutData(gd_btnOk);
        btnOk.setText("Ok");
        
        btnOk.addListener(SWT.Selection, e->shell.close());
    }
    
    private void removeLicense() {
		connector.sendCmd(UsbCmd.DELETE_LICENSE);
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		}
		connector.sendCmd(UsbCmd.RESET);
	}
}
