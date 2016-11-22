package com.rinke.solutions.pinball.ui;

import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class FileDialogDelegate implements FileChooser {

	FileDialog delegate;
	
	public FileDialogDelegate(Shell parent, int style) {
		delegate = new FileDialog(parent, style);
	}

	public void setFilterPath(String lastPath) {
		delegate.setFilterPath(lastPath);
	}

	public void setFilterExtensions(String[] strings) {
		delegate.setFilterExtensions(strings);
	}

	public void setFilterNames(String[] strings) {
		delegate.setFilterNames(strings);
	}

	public String open() {
		return delegate.open();
	}

	public String getFilterPath() {
		return delegate.getFilterPath();
	}

	public void setOverwrite(boolean b) {
		delegate.setOverwrite(b);
	}

	public void setFileName(String name) {
		delegate.setFileName(name);
	}

	@Override
	public String[] getFileNames() {
		return delegate.getFileNames();
	}

}
