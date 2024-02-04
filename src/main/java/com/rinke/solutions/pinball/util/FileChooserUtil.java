package com.rinke.solutions.pinball.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.Setter;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Scope;
import com.rinke.solutions.pinball.ui.FileChooser;
import com.rinke.solutions.pinball.ui.FileDialogDelegate;

@Bean(scope=Scope.PROTOTYPE)
public class FileChooserUtil {

	String lastPath;
	@Autowired Shell shell;

	// testability overridden by tests
	protected FileChooser createFileChooser(Shell shell, int flags) {
		return new FileDialogDelegate(shell, flags);
	}

	public List<String> chooseMulti(int type, String filename, String[] exts, String[] desc) {
		FileChooser fileChooser = createFileChooser(shell, type);
		fileChooser.setOverwrite(true);
		if( filename != null && filename.contains(File.separator) ) {
			lastPath = filename.substring(0, filename.lastIndexOf(File.separator));
			fileChooser.setFileName(basename(filename));
		} else {
			fileChooser.setFileName(filename);
		}
		
		if (lastPath != null)
			fileChooser.setFilterPath(lastPath);
		fileChooser.setFilterExtensions(exts);
		fileChooser.setFilterNames(desc);
		String returnedFilename = fileChooser.open();
		lastPath = fileChooser.getFilterPath();
		List<String> files = new ArrayList<String>();
		if( returnedFilename != null ) {
			String[] names = fileChooser.getFileNames();
			for (int i = 0, n = names.length; i < n; i++) {
				StringBuffer buf = new StringBuffer(fileChooser.getFilterPath());
				if (buf.charAt(buf.length() - 1) != File.separatorChar)
					buf.append(File.separatorChar);
				buf.append(names[i]);
				if (type == SWT.SAVE) {
					String extension = FilenameUtils.getExtension(names[i]);
					String extensionToAdd = exts[fileChooser.getFilterIndex()];
					if( extension != null && !extension.equals(extensionToAdd.substring(2))) {
						buf.append(".");
						buf.append(extensionToAdd.substring(2));
					}
				}
				files.add(buf.toString());
			}
		}
		return files;
	}
	
	String basename(String filename) {
		int i = filename.lastIndexOf(File.separator);
		return i==-1?filename:filename.substring(i+1);
	}

	public String choose(int type, String filename, String[] exts, String[] desc) {
		List<String> files = chooseMulti(type, filename, exts, desc);
		return files.size()>0?files.get(0):null;
	}

}
