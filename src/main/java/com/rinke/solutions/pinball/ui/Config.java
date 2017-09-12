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
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.LabelProviderAdapter;
import com.rinke.solutions.pinball.io.ConnectorFactory;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.ConnectionHandle;
import com.rinke.solutions.pinball.util.ApplicationProperties;

import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;

@Slf4j
public class Config extends Dialog {
    
	protected Object result;
    protected Shell shell;
    private DmdSize dmdSize;
    private String address;
    public boolean okPressed;

    private ComboViewer dmdSizeViewer;
	private Text pin2dmdHost;
	private Group grpDmd;
	private Group group;
	private Button btnOk;
	private Button btnAutosaveActive;
	private Spinner autosaveInterval;
	private Button btnCreateKeyFrame;
	private Spinner spinnerNoPlanes;
	private Button btnUseOldExport;
	private Button btnCreatePaletteAfter;
	private Button btnCreateBookmarkAfter;
    
    /**
     * Create the dialog.
     * @param parent
     * @param style
     */
    public Config(Shell parent) {
        super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
        setText("Configuration");
        dmdSize = DmdSize.fromOrdinal(ApplicationProperties.getInteger("dmdSize",0));
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
    public Object open(String address) {
        createContents();
        this.address = address;
        pin2dmdHost.setText(address!=null?address:"");
        
        btnAutosaveActive.setSelection(ApplicationProperties.getBoolean(ApplicationProperties.AUTOSAVE, false));
        autosaveInterval.setSelection(ApplicationProperties.getInteger(ApplicationProperties.AUTOSAVE_INTERVAL, 10));
        btnCreateKeyFrame.setSelection(ApplicationProperties.getBoolean(ApplicationProperties.AUTOKEYFRAME, false));
        spinnerNoPlanes.setSelection(ApplicationProperties.getInteger(ApplicationProperties.NOOFPLANES, 4));
        btnUseOldExport.setSelection(ApplicationProperties.getBoolean(ApplicationProperties.OLDEXPORT, false));
        btnCreatePaletteAfter.setSelection(ApplicationProperties.getBoolean(ApplicationProperties.ADDPALWHENCUT, false));
        btnCreateBookmarkAfter.setSelection(ApplicationProperties.getBoolean(ApplicationProperties.CREATEBOOKCUT, false));
        
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
        shell.setSize(533, 296);
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
        
        TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
        FormData fd_tabFolder = new FormData();
        fd_tabFolder.right = new FormAttachment(btnOk, 0, SWT.RIGHT);
        fd_tabFolder.bottom = new FormAttachment(0, 226);
        fd_tabFolder.top = new FormAttachment(0);
        fd_tabFolder.left = new FormAttachment(0);
        tabFolder.setLayoutData(fd_tabFolder);
        
        TabItem tbtmItem = new TabItem(tabFolder, SWT.NONE);
        tbtmItem.setText("General");
        
        Composite grpTest = new Composite(tabFolder, SWT.NONE);
        tbtmItem.setControl(grpTest);
        grpTest.setLayout(new FormLayout());
        
        group = new Group(grpTest, SWT.NONE);
        FormData fd_group = new FormData();
        group.setLayoutData(fd_group);
        group.setText("WiFi");
        group.setLayout(new GridLayout(3, false));
        
        Label label = new Label(group, SWT.NONE);
        GridData gd_label = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_label.widthHint = 83;
        label.setLayoutData(gd_label);
        label.setText("Adress");
        
        pin2dmdHost = new Text(group, SWT.BORDER);
        pin2dmdHost.setText("<dynamic>");
        GridData gd_text = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_text.widthHint = 267;
        pin2dmdHost.setLayoutData(gd_text);
        
		Button btnConnectBtn = new Button(group, SWT.NONE);
		btnConnectBtn.addListener(SWT.Selection, e->testConnect(pin2dmdHost.getText()));
		btnConnectBtn.setText("Connect");
		btnConnectBtn.addListener(SWT.Selection, e->testConnect(pin2dmdHost.getText()));
		new Label(group, SWT.NONE);
		
		Label label_1 = new Label(group, SWT.NONE);
		label_1.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		label_1.setText("Enter IP address or hostname for WiFi (default port is 9191)");
        
        grpDmd = new Group(grpTest, SWT.NONE);
        fd_group.top = new FormAttachment(grpDmd, 6);
        fd_group.left = new FormAttachment(grpDmd, 0, SWT.LEFT);
        FormData fd_grpDmd = new FormData();
        fd_grpDmd.top = new FormAttachment(0, 10);
        fd_grpDmd.left = new FormAttachment(0, 10);
        grpDmd.setLayoutData(fd_grpDmd);
        grpDmd.setText("DMD");
        
        dmdSizeViewer = new ComboViewer(grpDmd, SWT.READ_ONLY);
        Combo combo = dmdSizeViewer.getCombo();
        combo.setBounds(57, 10, 119, 22);
        dmdSizeViewer.setContentProvider(ArrayContentProvider.getInstance());
		dmdSizeViewer.setLabelProvider(new LabelProviderAdapter<DmdSize>(o -> o.label ));
		dmdSizeViewer.setInput(DmdSize.values());
		dmdSizeViewer.setSelection(new StructuredSelection(dmdSize));
		
		Label lblSize = new Label(grpDmd, SWT.RIGHT);
		lblSize.setBounds(10, 13, 41, 14);
		lblSize.setText("Size: ");
		
        Group grpAutosave = new Group(grpTest, SWT.NONE);
        FormData fd_grpAutosave = new FormData();
        fd_grpAutosave.top = new FormAttachment(grpDmd, 0, SWT.TOP);
        fd_grpAutosave.right = new FormAttachment(group, 0, SWT.RIGHT);
        fd_grpAutosave.left = new FormAttachment(grpDmd, 22);
        grpAutosave.setLayoutData(fd_grpAutosave);
        grpAutosave.setText("Autosave");
        
        btnAutosaveActive = new Button(grpAutosave, SWT.CHECK);
        btnAutosaveActive.setBounds(10, 10, 70, 18);
        btnAutosaveActive.setText("active");
        
        autosaveInterval = new Spinner(grpAutosave, SWT.BORDER);
        autosaveInterval.setBounds(69, 8, 52, 22);
        autosaveInterval.setIncrement(5);
        autosaveInterval.setMinimum(5);
        autosaveInterval.setMaximum(30);
        
        Label lblSec = new Label(grpAutosave, SWT.NONE);
        lblSec.setBounds(127, 13, 59, 14);
        lblSec.setText("min.");
        
        TabItem tbtmSettings = new TabItem(tabFolder, SWT.NONE);
        tbtmSettings.setText("Settings");
        
        Composite grpFoo = new Composite(tabFolder, SWT.NONE);
        tbtmSettings.setControl(grpFoo);
        grpFoo.setLayout(new FormLayout());
        
        Group grpCutting = new Group(grpFoo, SWT.NONE);
        grpCutting.setText("Cutting");
        FormData fd_grpCutting = new FormData();
        fd_grpCutting.top = new FormAttachment(0);
        fd_grpCutting.left = new FormAttachment(0);
        fd_grpCutting.bottom = new FormAttachment(100, -42);
        fd_grpCutting.right = new FormAttachment(100, -228);
        grpCutting.setLayoutData(fd_grpCutting);
        
        btnCreateKeyFrame = new Button(grpCutting, SWT.CHECK);
        btnCreateKeyFrame.setBounds(10, 10, 228, 18);
        btnCreateKeyFrame.setText("create key frame after cutting");
        
        btnCreatePaletteAfter = new Button(grpCutting, SWT.CHECK);
        btnCreatePaletteAfter.setBounds(10, 34, 241, 18);
        btnCreatePaletteAfter.setText("create palette after cutting");
        
        btnCreateBookmarkAfter = new Button(grpCutting, SWT.CHECK);
        btnCreateBookmarkAfter.setBounds(10, 58, 241, 18);
        btnCreateBookmarkAfter.setText("create bookmark after cutting");
        
        Label lblNumberOfPlanes = new Label(grpCutting, SWT.NONE);
        lblNumberOfPlanes.setBounds(60, 87, 192, 14);
        lblNumberOfPlanes.setText("Number of planes when cutting");
        
        spinnerNoPlanes = new Spinner(grpCutting, SWT.BORDER);
        spinnerNoPlanes.setBounds(10, 82, 44, 22);
        spinnerNoPlanes.setMinimum(2);
        spinnerNoPlanes.setMaximum(15);
        
        Group grpExport = new Group(grpFoo, SWT.NONE);
        grpExport.setText("Export");
        FormData fd_grpExport = new FormData();
        fd_grpExport.bottom = new FormAttachment(grpCutting, 0, SWT.BOTTOM);
        fd_grpExport.top = new FormAttachment(0);
        fd_grpExport.right = new FormAttachment(100, -10);
        fd_grpExport.left = new FormAttachment(grpCutting, 6);
        grpExport.setLayoutData(fd_grpExport);
        
        btnUseOldExport = new Button(grpExport, SWT.CHECK);
        btnUseOldExport.setBounds(10, 10, 188, 18);
        btnUseOldExport.setText("use old export format");
        
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
        ApplicationProperties.put(ApplicationProperties.AUTOSAVE, btnAutosaveActive.getSelection());
        ApplicationProperties.put(ApplicationProperties.AUTOSAVE_INTERVAL, autosaveInterval.getSelection()); 
        ApplicationProperties.put(ApplicationProperties.AUTOKEYFRAME, btnCreateKeyFrame.getSelection()); 
        ApplicationProperties.put(ApplicationProperties.NOOFPLANES, spinnerNoPlanes.getSelection());
        ApplicationProperties.put(ApplicationProperties.OLDEXPORT, btnUseOldExport.getSelection());
        ApplicationProperties.put(ApplicationProperties.ADDPALWHENCUT, btnCreatePaletteAfter.getSelection());
        ApplicationProperties.put(ApplicationProperties.CREATEBOOKCUT, btnCreateBookmarkAfter.getSelection());
		shell.close();
	}
}
