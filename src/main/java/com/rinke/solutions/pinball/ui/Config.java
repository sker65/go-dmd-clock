package com.rinke.solutions.pinball.ui;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.widgets.Text;

import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.LabelProviderAdapter;
import com.rinke.solutions.pinball.io.ConnectorFactory;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.io.Pin2DmdConnector.ConnectionHandle;
import com.rinke.solutions.pinball.util.ApplicationProperties;

@Slf4j
public class Config extends Dialog {
    
    protected Object result;
    protected Shell shell;
    private DmdSize dmdSize;
    private String address;
    public boolean okPressed;

    private ComboViewer dmdSizeViewer;
	private Text pin2dmdHost;
    
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
        shell.setSize(489, 243);
        shell.setText("Configuration");
        shell.setLayout(new FormLayout());
        
        Button btnOk = new Button(shell, SWT.NONE);
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
        
        Group grpDmd = new Group(shell, SWT.NONE);
        grpDmd.setText("DMD");
        FormData fd_grpDmd = new FormData();
        fd_grpDmd.top = new FormAttachment(0, 10);
        fd_grpDmd.right = new FormAttachment(btnOk, 0, SWT.RIGHT);
        fd_grpDmd.left = new FormAttachment(0, 10);
        fd_grpDmd.bottom = new FormAttachment(0, 80);
        grpDmd.setLayoutData(fd_grpDmd);
        
        dmdSizeViewer = new ComboViewer(grpDmd, SWT.READ_ONLY);
        Combo combo = dmdSizeViewer.getCombo();
        combo.setBounds(57, 10, 119, 22);
        dmdSizeViewer.setContentProvider(ArrayContentProvider.getInstance());
		dmdSizeViewer.setLabelProvider(new LabelProviderAdapter<DmdSize>(o -> o.label ));
		dmdSizeViewer.setInput(DmdSize.values());
		dmdSizeViewer.setSelection(new StructuredSelection(dmdSize));
		
		Group group = new Group(shell, SWT.NONE);
		group.setText("WiFi");
		group.setLayout(new GridLayout(3, false));
		FormData fd_group = new FormData();
		fd_group.right = new FormAttachment(100, -10);
		fd_group.top = new FormAttachment(grpDmd, 7);
		fd_group.left = new FormAttachment(grpDmd, 0, SWT.LEFT);
		
		Label lblSize = new Label(grpDmd, SWT.RIGHT);
		lblSize.setBounds(10, 13, 41, 14);
		lblSize.setText("Size: ");
		group.setLayoutData(fd_group);
		
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
        
        FormData fd_grpConfig = new FormData();
        fd_grpConfig.bottom = new FormAttachment(100, -292);
    }

	private void cancel() {
		okPressed = true;
		shell.close();
	}

	private void ok() {
		okPressed = true;
		dmdSize = (DmdSize) ((StructuredSelection) dmdSizeViewer.getSelection()).getFirstElement();
		shell.close();
	}
}
