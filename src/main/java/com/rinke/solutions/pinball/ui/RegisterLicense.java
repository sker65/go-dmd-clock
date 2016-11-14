package com.rinke.solutions.pinball.ui;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.api.LicenseManager;
import com.rinke.solutions.pinball.api.LicenseManager.Capability;
import com.rinke.solutions.pinball.api.LicenseManager.VerifyResult;
import com.rinke.solutions.pinball.api.LicenseManagerFactory;
import com.rinke.solutions.pinball.io.ConnectorFactory;
import com.rinke.solutions.pinball.io.Pin2DmdConnector;
import com.rinke.solutions.pinball.io.UsbConnector;


public class RegisterLicense extends Dialog {
	
	private static Logger LOG = LoggerFactory.getLogger(RegisterLicense.class);

	private Shell shell;
	private Text licFileText;
	private List capList;
	private String lastPath;
	String filename;
	LicenseManager licManager = LicenseManagerFactory.getInstance();

	public RegisterLicense(Shell parent) {
		super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK
				| SWT.APPLICATION_MODAL);
		setText("Register License");
		this.shell = parent;
	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public Object open() {
		createContents();
		load();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return null;
	}

	/**
	 * Create contents of the dialog.
	 */
	void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(377, 162);
		shell.setText("Register License");
		shell.setLayout(new FormLayout());

		Button btnOk = new Button(shell, SWT.NONE);
		FormData fd_btnOk = new FormData();
		fd_btnOk.bottom = new FormAttachment(100, -10);
		fd_btnOk.right = new FormAttachment(100, -10);
		btnOk.setLayoutData(fd_btnOk);
		btnOk.setText("Ok");
		btnOk.addListener(SWT.Selection, e -> save());

		Button btnCancel = new Button(shell, SWT.NONE);
		FormData fd_btnCancel = new FormData();
		fd_btnCancel.top = new FormAttachment(btnOk, 0, SWT.TOP);
		fd_btnCancel.right = new FormAttachment(btnOk, -6);
		btnCancel.setLayoutData(fd_btnCancel);
		btnCancel.setText("Cancel");
		btnCancel.addListener(SWT.Selection, e -> shell.close());

		Label lblLicenseFile = new Label(shell, SWT.NONE);
		FormData fd_lblLicenseFile = new FormData();
		fd_lblLicenseFile.top = new FormAttachment(0, 10);
		fd_lblLicenseFile.left = new FormAttachment(0, 10);
		lblLicenseFile.setLayoutData(fd_lblLicenseFile);
		lblLicenseFile.setText("License File:");

		licFileText = new Text(shell, SWT.BORDER);
		FormData fd_text = new FormData();
		fd_text.top = new FormAttachment(0, 10);
		fd_text.left = new FormAttachment(lblLicenseFile, 6);
		licFileText.setLayoutData(fd_text);

		Button btnChoose = new Button(shell, SWT.NONE);
		fd_text.right = new FormAttachment(btnChoose, -6);
		FormData fd_btnChoose = new FormData();
		fd_btnChoose.top = new FormAttachment(0, 6);
		fd_btnChoose.left = new FormAttachment(0, 285);
		btnChoose.setLayoutData(fd_btnChoose);
		btnChoose.setText("Choose");
		btnChoose.addListener(SWT.Selection, e -> choose());

		capList = new List(shell, SWT.BORDER);
		FormData fd_list = new FormData();
		fd_list.top = new FormAttachment(licFileText, 14);
		fd_list.bottom = new FormAttachment(100, -22);
		fd_list.left = new FormAttachment(0, 10);
		fd_list.right = new FormAttachment(0, 225);
		capList.setLayoutData(fd_list);
		
		Button btnUpload = new Button(shell, SWT.NONE);
		FormData fd_btnUpload = new FormData();
		fd_btnUpload.top = new FormAttachment(btnChoose, 6);
		fd_btnUpload.left = new FormAttachment(btnCancel, 0, SWT.LEFT);
		btnUpload.setLayoutData(fd_btnUpload);
		btnUpload.setText("Upload");
		btnUpload.addListener(SWT.Selection, e->uploadLicense());

	}

	Object uploadLicense() {
		String licFile = licFileText.getText();
		LOG.info("uploading license file: {}", licFile);
		if( !StringUtils.isEmpty(licFile)) {
			Pin2DmdConnector connector = ConnectorFactory.create(null);
			connector.installLicense(licFile);
		}
		return null;
	}

	protected FileChooser createFileChooser(Shell shell, int flags) {
		return new FileDialogDelegate(shell, flags);
	}

	private Object choose() {
		FileChooser fileChooser = createFileChooser(shell, SWT.OPEN);
		// fileChooser.setFileName("pin2dmd.dat");
		if (lastPath != null)
			fileChooser.setFilterPath(lastPath);
		fileChooser.setFilterExtensions(new String[] { "*.key" });
		fileChooser.setFilterNames(new String[] { "license file" });
		filename = fileChooser.open();
		lastPath = fileChooser.getFilterPath();
		if (filename != null) {
			licFileText.setText(filename);
			try {
				LOG.info("loading license file: {}", filename);
				verifyAndPopulateCaps(licManager.verify(filename));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	Object load() {
		licManager.load();
		if (licManager.getLicenseFile() != null) {
			filename = licManager.getLicenseFile();
			licFileText.setText(filename);
			verifyAndPopulateCaps(licManager.getLicense());
		}
		return null;
	}

	private void verifyAndPopulateCaps(VerifyResult res) {
		capList.removeAll();
		if (res != null && res.valid) {
			for (Capability c : res.capabilities) {
				capList.add(c.name().toLowerCase());
			}
		} else {
			capList.add("invalid key");
		}
	}

	Object save() {
		if( filename != null && licManager.getLicense().valid ) {
			licManager.save(filename);
		}
		shell.close();
		return null;
	}
}
