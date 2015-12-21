package com.rinke.solutions.pinball.ui;

import java.io.FileOutputStream;
import java.io.IOException;

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

import com.rinke.solutions.pinball.model.DefaultPalette;
import com.rinke.solutions.pinball.model.DeviceMode;

public class DeviceConfig extends Dialog {
    
    private static final Logger LOG = LoggerFactory.getLogger(DeviceConfig.class);

    protected Object result;
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
            writeDeviceConfig(filename, deviceModeCombo.getSelectionIndex(), deviceModeCombo.getSelectionIndex());
        }
        shell.close();
    }

    private void writeDeviceConfig(String filename, int mode, int defPalette) {
        byte[] dummy = {0x0F,0x0A,0x0F,0x0C,0x0F,0x00,0x0F,0x0F,0,0};
        try(FileOutputStream 
            fos = new FileOutputStream(filename) ) {
            fos.write(mode);
            fos.write(defPalette);
            fos.write(dummy);
        } catch(IOException e) {
            LOG.error("problems writing {}", filename,e);
            throw new RuntimeException("error writing "+filename, e);
        }
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
        shell.setSize(448, 162);
        shell.setText("Device Configuration");
        shell.setLayout(new FormLayout());
        
        Group grpConfig = new Group(shell, SWT.NONE);
        FormData fd_grpConfig = new FormData();
        fd_grpConfig.top = new FormAttachment(0, 10);
        fd_grpConfig.left = new FormAttachment(0, 10);
        fd_grpConfig.bottom = new FormAttachment(0, 129);
        fd_grpConfig.right = new FormAttachment(0, 440);
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
        comboDefaultPalette.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboViewerDefaultPalette.setContentProvider(ArrayContentProvider.getInstance());
        comboViewerDefaultPalette.setInput(DefaultPalette.values());
        comboDefaultPalette.select(0);
        
        Button btnCancel = new Button(grpConfig, SWT.NONE);
        btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnCancel.setText("Cancel");
        btnCancel.addListener(SWT.Selection, e->shell.close());
    }
}
