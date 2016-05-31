package com.rinke.solutions.pinball.util;

import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.pinball.ui.FileChooser;
import com.rinke.solutions.pinball.ui.FileDialogDelegate;

public class FileChooserUtil {
	
	String lastPath;
	Shell shell;

	public FileChooserUtil(Shell shell) {
		super();
		this.shell = shell;
	}
	
	// testability overridden by tests
	protected FileChooser createFileChooser(Shell shell, int flags) {
		return new FileDialogDelegate(shell, flags);
	}

	public String choose(int type, String filename, String[] exts, String[] desc) {
		FileChooser fileChooser = createFileChooser(shell, type);
		fileChooser.setOverwrite(true);
		fileChooser.setFileName(filename);
		if (lastPath != null)
			fileChooser.setFilterPath(lastPath);
		fileChooser.setFilterExtensions(exts);
		fileChooser.setFilterNames(desc);
		String returnedFilename = fileChooser.open();
		lastPath = fileChooser.getFilterPath();
		return returnedFilename;
	}

}
