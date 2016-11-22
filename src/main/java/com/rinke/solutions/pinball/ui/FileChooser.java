package com.rinke.solutions.pinball.ui;

/** wrapper interface for swt file dialog for testing.*/
public interface FileChooser {

	void setFilterPath(String lastPath);

	void setFilterExtensions(String[] strings);

	void setFilterNames(String[] strings);

	String open();

	String getFilterPath();

	void setOverwrite(boolean b);

	void setFileName(String name);

	String[] getFileNames();


}
