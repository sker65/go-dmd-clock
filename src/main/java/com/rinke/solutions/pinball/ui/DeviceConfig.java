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
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Scope;
import com.rinke.solutions.pinball.model.DefaultPalette;
import com.rinke.solutions.pinball.model.DeviceMode;
import com.rinke.solutions.pinball.view.View;

@Slf4j
@Bean(name="deviceConfig", scope=Scope.PROTOTYPE)
public class DeviceConfig extends Dialog implements View {
    
    protected Shell shell;
    private String lastPath;
    private ComboViewer deviceModecomboViewer;
    private ComboViewer comboViewerDefaultPalette;
    private Combo deviceModeCombo;
    private Combo comboDefaultPalette;
    
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
            		btnInvertClock.getSelection(), scBrightness.getSelection(), rgbSeqCombo.getSelectionIndex());
        }
        shell.close();
    }

    void writeDeviceConfig(String filename, int mode, int defPalette, boolean invertClock, int brightness, int rgbSeq) {
        byte[] dummy = {
        		0x0F,0x0A,0x0F,0x0C, 0x0F,0x00,0x0F,0x0F, // dummy dmd sig
        		0,0, 0,0,0,0, 0,0,0,0 };
        
        byte[] foo = new byte[10];
        
        try(FileOutputStream 
            fos = new FileOutputStream(filename) ) {
            fos.write(mode);
            fos.write(defPalette);
            fos.write(dummy);
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
	private ComboViewer rgbSeqComboViewer;
	private Combo rgbSeqCombo;

    /**
     * Open the dialog.
     * @return the result
     */
    public void open() {
        createContents();
        
        Button btnOk = new Button(shell, SWT.NONE);
        fd_grpConfig.bottom = new FormAttachment(100, -47);
        FormData fd_btnOk = new FormData();
        fd_btnOk.top = new FormAttachment(grpConfig, 9);
        fd_btnOk.right = new FormAttachment(grpConfig, 0, SWT.RIGHT);
        
        Label lblRgbSeq = new Label(grpConfig, SWT.NONE);
        GridData gd_lblRgbSeq = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblRgbSeq.heightHint = 27;
        lblRgbSeq.setLayoutData(gd_lblRgbSeq);
        lblRgbSeq.setText("RGB Seq");
        
        rgbSeqComboViewer = new ComboViewer(grpConfig, SWT.NONE);
        rgbSeqCombo = rgbSeqComboViewer.getCombo();
        rgbSeqCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        rgbSeqComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        rgbSeqComboViewer.setInput(RgbSequence.values());
        rgbSeqCombo.select(0);

        new Label(grpConfig, SWT.NONE);
        
        fd_btnOk.left = new FormAttachment(0, 405);
        btnOk.setLayoutData(fd_btnOk);
        btnOk.setText("Ok");
        btnOk.addListener(SWT.Selection, e->shell.close());
        
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
        shell.setSize(480, 270);
        shell.setText("Device Configuration");
        shell.setLayout(new FormLayout());
        
        grpConfig = new Group(shell, SWT.NONE);
        fd_grpConfig = new FormData();
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
        
        Label lblInvertClock = new Label(grpConfig, SWT.NONE);
        lblInvertClock.setText("Enhancer");
        
        btnInvertClock = new Button(grpConfig, SWT.CHECK);
        new Label(grpConfig, SWT.NONE);
        
        Label lblBrightness = new Label(grpConfig, SWT.NONE);
        lblBrightness.setText("Brightness");
        
        scBrightness = new Scale(grpConfig, SWT.NONE);
        GridData gd_scale = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_scale.heightHint = 43;
        scBrightness.setLayoutData(gd_scale);
        scBrightness.setMinimum(0);
        scBrightness.setMaximum(255);
        scBrightness.setIncrement(1);
        
        new Label(grpConfig, SWT.NONE);
    }
}
