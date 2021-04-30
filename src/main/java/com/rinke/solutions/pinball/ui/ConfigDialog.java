package com.rinke.solutions.pinball.ui;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Scope;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.LabelProviderAdapter;
import com.rinke.solutions.pinball.io.ConnectorFactory;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.ConnectionHandle;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.View;

@Slf4j
@Bean(name="configDialog", scope=Scope.PROTOTYPE)
public class ConfigDialog extends Dialog implements View {
    
    protected Shell shell;
    private DmdSize dmdSize;

    public boolean okPressed;

    private ComboViewer dmdSizeViewer;
    private Spinner spinnerNoColors;
	private Group grpDmd;
	private Button btnOk;
	private Button btnAutosaveActive;
	private Spinner autosaveInterval;
	private Button btnCreateKeyFrame;
	private Spinner spinnerNoPlanes;
	private Button btnUseOldExport;
	private Button btnCreatePaletteAfter;
	private Button btnCreateBookmarkAfter;
	private Button btnBackupOnSave;
	
	@Value(key=Config.PIN2DMD_ADRESS)
    private String address;
	
	@Autowired Config config;
	@Autowired MessageUtil messageUtil;
	private Button btnNoExportWarnings;
	private Button btnNoQuitWarning;
	private TabFolder tabFolder;
    
    /**
     * Create the dialog.
     * @param parent
     * @param style
     */
    public ConfigDialog(Shell parent) {
        super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
        setText("Configuration");
    }

    public String getPin2DmdHost() {
    	return address;
    }
    
    public DmdSize getDmdSize() {
    	return dmdSize;
    }

	private void testConnect(String address) {
		Pin2DmdConnector connector = ConnectorFactory.create(address);
		ConnectionHandle handle = connector.connect(address);
		connector.release(handle);
		this.address = address;
	}

    /**
     * Open the dialog.
     * @return the result
     */
    public void open() {
        
    	dmdSize = DmdSize.fromOrdinal(config.getInteger(Config.DMDSIZE,0));

        createContents();
        btnAutosaveActive.setSelection(config.getBoolean(Config.AUTOSAVE, false));
        autosaveInterval.setSelection(config.getInteger(Config.AUTOSAVE_INTERVAL, 10));
        btnCreateKeyFrame.setSelection(config.getBoolean(Config.AUTOKEYFRAME, false));
        spinnerNoPlanes.setSelection(config.getInteger(Config.NOOFPLANES, 4));
        spinnerNoColors.setSelection(config.getInteger(Config.NOOFCOLORS, 16));
        btnUseOldExport.setSelection(config.getBoolean(Config.OLDEXPORT, false));
        btnCreatePaletteAfter.setSelection(config.getBoolean(Config.ADDPALWHENCUT, false));
        btnCreateBookmarkAfter.setSelection(config.getBoolean(Config.CREATEBOOKCUT, false));
        btnBackupOnSave.setSelection(config.getBoolean(Config.BACKUP, false));
        btnNoExportWarnings.setSelection(config.getBoolean(Config.NO_EXPORT_WARNING, false));
        btnNoQuitWarning.setSelection(config.getBoolean(Config.NO_QUIT_WARNING, false));
        
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
        shell.setSize(533, 409);
        shell.setText("Configuration");
        shell.setLayout(new FormLayout());
        
        btnOk = new Button(shell, SWT.NONE);
        FormData fd_btnOk = new FormData();
        fd_btnOk.bottom = new FormAttachment(100, -10);
        fd_btnOk.right = new FormAttachment(100, -10);
        btnOk.setLayoutData(fd_btnOk);
        btnOk.setText("Ok");
        btnOk.addListener(SWT.Selection, e->ok());
        
        Button btnCancel = new Button(shell, SWT.NONE);
        FormData fd_btnCancel = new FormData();
        fd_btnCancel.top = new FormAttachment(btnOk, 0, SWT.TOP);
        fd_btnCancel.right = new FormAttachment(btnOk, -9);
        btnCancel.setLayoutData(fd_btnCancel);
        btnCancel.setText("Cancel");
        btnCancel.addListener(SWT.Selection, e->cancel());
        
        tabFolder = new TabFolder(shell, SWT.NONE);
        FormData fd_tabFolder = new FormData();
        fd_tabFolder.right = new FormAttachment(btnOk, 0, SWT.RIGHT);
        fd_tabFolder.bottom = new FormAttachment(0, 324);
        fd_tabFolder.top = new FormAttachment(0);
        fd_tabFolder.left = new FormAttachment(0);
        tabFolder.setLayoutData(fd_tabFolder);
        
        TabItem tbtmItem = new TabItem(tabFolder, SWT.NONE);
        tbtmItem.setText("General");
        
        Composite grpTest = new Composite(tabFolder, SWT.NONE);
        tbtmItem.setControl(grpTest);
        grpTest.setLayout(new FormLayout());
        
        grpDmd = new Group(grpTest, SWT.NONE);
        FormData fd_grpDmd = new FormData();
        fd_grpDmd.right = new FormAttachment(0, 223);
        fd_grpDmd.bottom = new FormAttachment(100, -34);
        fd_grpDmd.top = new FormAttachment(0, 10);
        fd_grpDmd.left = new FormAttachment(0, 10);
        grpDmd.setLayoutData(fd_grpDmd);
        grpDmd.setText("Project");
        
        dmdSizeViewer = new ComboViewer(grpDmd, SWT.READ_ONLY);
        Combo combo = dmdSizeViewer.getCombo();
        combo.setBounds(69, 16, 127, 23);
        dmdSizeViewer.setContentProvider(ArrayContentProvider.getInstance());
		dmdSizeViewer.setLabelProvider(new LabelProviderAdapter<DmdSize>(o -> o.label ));
		dmdSizeViewer.setInput(DmdSize.values());
		dmdSizeViewer.setSelection(new StructuredSelection(dmdSize));
		
		Label lblSize = new Label(grpDmd, SWT.RIGHT);
		lblSize.setBounds(10, 19, 53, 14);
		lblSize.setText("DMDSize: ");
		
        Group grpAutosave = new Group(grpTest, SWT.NONE);
        FormData fd_grpAutosave = new FormData();
        fd_grpAutosave.left = new FormAttachment(grpDmd, 15);
        fd_grpAutosave.right = new FormAttachment(100, -28);
        fd_grpAutosave.bottom = new FormAttachment(100, -34);
        fd_grpAutosave.top = new FormAttachment(0, 10);
        
        Label lblColor = new Label(grpDmd, SWT.RIGHT);
        lblColor.setText("Colors: ");
        lblColor.setBounds(10, 48, 41, 14);
        
        spinnerNoColors = new Spinner(grpDmd, SWT.BORDER);
        spinnerNoColors.setBounds(69, 45, 127, 23);
        spinnerNoColors.setMinimum(16);
        spinnerNoColors.setMaximum(64);
        spinnerNoColors.setIncrement(48);;
        
        grpAutosave.setLayoutData(fd_grpAutosave);
        grpAutosave.setText("Save");
        
        btnAutosaveActive = new Button(grpAutosave, SWT.CHECK);
        btnAutosaveActive.setBounds(10, 21, 106, 18);
        btnAutosaveActive.setText("autosave active");
        
        autosaveInterval = new Spinner(grpAutosave, SWT.BORDER);
        autosaveInterval.setBounds(122, 20, 52, 22);
        autosaveInterval.setIncrement(5);
        autosaveInterval.setMinimum(5);
        autosaveInterval.setMaximum(30);
        
        Label lblSec = new Label(grpAutosave, SWT.NONE);
        lblSec.setBounds(180, 23, 40, 14);
        lblSec.setText("min.");
        
        btnBackupOnSave = new Button(grpAutosave, SWT.CHECK);
        btnBackupOnSave.setBounds(10, 45, 140, 18);
        btnBackupOnSave.setText("backup on save");
        
        TabItem tbtmSettings = new TabItem(tabFolder, SWT.NONE);
        tbtmSettings.setText("Settings");
        
        Composite grpFoo = new Composite(tabFolder, SWT.NONE);
        tbtmSettings.setControl(grpFoo);
        grpFoo.setLayout(new FormLayout());
        
        Group grpCutting = new Group(grpFoo, SWT.NONE);
        grpCutting.setText("Cutting");
        FormData fd_grpCutting = new FormData();
        fd_grpCutting.bottom = new FormAttachment(100, -138);
        fd_grpCutting.top = new FormAttachment(0);
        fd_grpCutting.left = new FormAttachment(0);
        fd_grpCutting.right = new FormAttachment(100, -228);
        grpCutting.setLayoutData(fd_grpCutting);
        
        btnCreateKeyFrame = new Button(grpCutting, SWT.CHECK);
        btnCreateKeyFrame.setBounds(10, 20, 228, 18);
        btnCreateKeyFrame.setText("create key frame after cutting");
        
        btnCreatePaletteAfter = new Button(grpCutting, SWT.CHECK);
        btnCreatePaletteAfter.setBounds(10, 44, 241, 18);
        btnCreatePaletteAfter.setText("create palette after cutting");
        
        btnCreateBookmarkAfter = new Button(grpCutting, SWT.CHECK);
        btnCreateBookmarkAfter.setBounds(10, 68, 241, 18);
        btnCreateBookmarkAfter.setText("create bookmark after cutting");
        
        Label lblNumberOfPlanes = new Label(grpCutting, SWT.NONE);
        lblNumberOfPlanes.setBounds(60, 97, 192, 14);
        lblNumberOfPlanes.setText("Number of planes when cutting");
        
        spinnerNoPlanes = new Spinner(grpCutting, SWT.BORDER);
        spinnerNoPlanes.setBounds(10, 92, 44, 22);
        spinnerNoPlanes.setMinimum(2);
        spinnerNoPlanes.setMaximum(15);
        
        Group grpExport = new Group(grpFoo, SWT.NONE);
        grpExport.setText("Export / Save");
        FormData fd_grpExport = new FormData();
        fd_grpExport.right = new FormAttachment(100, -10);
        fd_grpExport.left = new FormAttachment(grpCutting, 6);
        fd_grpExport.top = new FormAttachment(0);
        grpExport.setLayoutData(fd_grpExport);
        
        btnUseOldExport = new Button(grpExport, SWT.CHECK);
        btnUseOldExport.setBounds(10, 22, 188, 18);
        btnUseOldExport.setText("use old export format");
        
        Group grpGeneral = new Group(grpFoo, SWT.NONE);
        fd_grpExport.bottom = new FormAttachment(grpGeneral, -28);
        grpGeneral.setText("General");
        FormData fd_grpGeneral = new FormData();
        fd_grpGeneral.right = new FormAttachment(100, -10);
        fd_grpGeneral.left = new FormAttachment(grpCutting, 6);
        fd_grpGeneral.bottom = new FormAttachment(100, -10);
        fd_grpGeneral.top = new FormAttachment(0, 120);
        
        Combo comboAniVersion = new Combo(grpExport, SWT.NONE);
        comboAniVersion.setBounds(10, 41, 40, 22);
        comboAniVersion.setItems(new String[]{"1","2","3","4","5","6","7","8"});
        
        Label lblAniFormatVersion = new Label(grpExport, SWT.NONE);
        lblAniFormatVersion.setBounds(56, 46, 142, 14);
        lblAniFormatVersion.setText("Ani Format version ");
        grpGeneral.setLayoutData(fd_grpGeneral);
        
        btnNoQuitWarning = new Button(grpGeneral, SWT.CHECK);
        btnNoQuitWarning.setBounds(10, 20, 131, 18);
        btnNoQuitWarning.setText("no quit warning");
        
        btnNoExportWarnings = new Button(grpGeneral, SWT.CHECK);
        btnNoExportWarnings.setBounds(10, 44, 149, 18);
        btnNoExportWarnings.setText("no export warnings");
        
        Group grpInterpolation = new Group(grpFoo, SWT.NONE);
        grpInterpolation.setText("Interpolation");
        FormData fd_grpInterpolation = new FormData();
        fd_grpInterpolation.bottom = new FormAttachment(grpCutting, 98, SWT.BOTTOM);
        fd_grpInterpolation.top = new FormAttachment(grpCutting, 6);
        fd_grpInterpolation.right = new FormAttachment(grpCutting, 0, SWT.RIGHT);
        fd_grpInterpolation.left = new FormAttachment(0);
        grpInterpolation.setLayoutData(fd_grpInterpolation);
        
        Button btnPixel = new Button(grpInterpolation, SWT.RADIO);
        btnPixel.setBounds(10, 10, 65, 18);
        btnPixel.setText("Pixel");
        
        Button btnPalette = new Button(grpInterpolation, SWT.RADIO);
        btnPalette.setBounds(81, 10, 90, 18);
        btnPalette.setText("Palette");
        
        Button btnForceNearestKeyframe = new Button(grpInterpolation, SWT.CHECK);
        btnForceNearestKeyframe.setBounds(10, 36, 185, 18);
        btnForceNearestKeyframe.setText("Force nearest keyframe");
        
        FormData fd_grpConfig = new FormData();
        fd_grpConfig.bottom = new FormAttachment(100, -292);
    }

	private void cancel() {
		log.info("cancel pressed");
		okPressed = false;
		shell.close();
	}

	private void ok() {
		log.info("ok pressed");
		okPressed = true;
		dmdSize = (DmdSize) ((StructuredSelection) dmdSizeViewer.getSelection()).getFirstElement();
		if (spinnerNoPlanes.getSelection() == 4 && spinnerNoColors.getSelection() == 64)
			spinnerNoPlanes.setSelection(6);
		if (spinnerNoPlanes.getSelection() == 6 && spinnerNoColors.getSelection() == 16)
			spinnerNoPlanes.setSelection(4);
        config.put(Config.AUTOSAVE, btnAutosaveActive.getSelection());
        config.put(Config.AUTOSAVE_INTERVAL, autosaveInterval.getSelection()); 
        config.put(Config.AUTOKEYFRAME, btnCreateKeyFrame.getSelection()); 
        config.put(Config.NOOFPLANES, spinnerNoPlanes.getSelection());
        config.put(Config.NOOFCOLORS, spinnerNoColors.getSelection());
        config.put(Config.OLDEXPORT, btnUseOldExport.getSelection());
        config.put(Config.ADDPALWHENCUT, btnCreatePaletteAfter.getSelection());
        config.put(Config.CREATEBOOKCUT, btnCreateBookmarkAfter.getSelection());
        config.put(Config.BACKUP, btnBackupOnSave.getSelection());
        config.put(Config.NO_QUIT_WARNING, btnNoQuitWarning.getSelection());
        config.put(Config.NO_EXPORT_WARNING, btnNoExportWarnings.getSelection());
		messageUtil.warn(SWT.ICON_ERROR | SWT.OK,
				"Config changes",
				"The configuration has changed ! Please reload the editor. ");
		shell.close();
	}
}
