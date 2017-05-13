package com.rinke.solutions.pinball.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.ui.FileChooser;
import com.rinke.solutions.pinball.ui.FileDialogDelegate;

@Bean
public class FileChooserUtil {

	String lastPath;
	Shell shell;

	public FileChooserUtil(Shell shell) {
		super();
		this.shell = shell;
	}
	
	public static String replaceExtensionTo(String newExt, String filename) {
		int p = filename.lastIndexOf(".");
		if (p != -1)
			return filename.substring(0, p) + "." + newExt;
		return filename;
	}
	
	public static String buildRelFilename(String parent, String file) {
		if( file.contains(File.separator)) return file;
		return new File(parent).getParent() + File.separator + new File(file).getName();
	}
	


	// testability overridden by tests
	protected FileChooser createFileChooser(Shell shell, int flags) {
		return new FileDialogDelegate(shell, flags);
	}

	public List<String> chooseMulti(int type, String filename, String[] exts, String[] desc) {
		FileChooser fileChooser = createFileChooser(shell, type);
		fileChooser.setOverwrite(true);
		fileChooser.setFileName(filename);
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
				files.add(buf.toString());
			}
		}
		return files;
	}
	
	public String choose(int type, String filename, String[] exts, String[] desc) {
		List<String> files = chooseMulti(type, filename, exts, desc);
		return files.size()>0?files.get(0):null;
	}

}
