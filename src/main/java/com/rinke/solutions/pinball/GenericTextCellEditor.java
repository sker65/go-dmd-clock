package com.rinke.solutions.pinball;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

public class GenericTextCellEditor<T> extends EditingSupport {

	private TextCellEditor editor;
	private StringExtractor<T> ext;
	private StringSetter<T> setter;
	
	@FunctionalInterface
    public interface StringExtractor<T> {
        String get(T o);
    }

	@FunctionalInterface
    public interface StringSetter<T> {
        void setString(T o, String v);
    }

	public GenericTextCellEditor(ColumnViewer viewer, StringExtractor<T> ext, StringSetter<T> setter ) {
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
		return ext.get((T) element);
	}

	@Override
	protected void setValue(Object element, Object value) {
		setter.setString((T) element, (String)value);
		getViewer().update(element, null);
	}


}
