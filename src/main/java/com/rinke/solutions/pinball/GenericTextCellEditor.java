package com.rinke.solutions.pinball;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

public class GenericTextCellEditor extends EditingSupport {

	private TextCellEditor editor;
	private StringExtractor ext;
	private StringSetter setter;
	
	@FunctionalInterface
    public interface StringExtractor {
        String get(Object o);
    }

	@FunctionalInterface
    public interface StringSetter {
        void setString(Object o, String v);
    }

	public GenericTextCellEditor(ColumnViewer viewer, StringExtractor ext, StringSetter setter ) {
		super(viewer);
		Composite parent = (Composite) viewer.getControl();
		this.editor = new TextCellEditor(parent);
		this.ext = ext;
		this.setter = setter;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return editor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		return ext.get(element);
	}

	@Override
	protected void setValue(Object element, Object value) {
		setter.setString(element, (String)value);
		getViewer().update(element, null);
	}


}
