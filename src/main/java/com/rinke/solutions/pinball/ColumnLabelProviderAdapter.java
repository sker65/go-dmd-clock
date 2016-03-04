package com.rinke.solutions.pinball;

import org.eclipse.jface.viewers.ColumnLabelProvider;

public class ColumnLabelProviderAdapter extends ColumnLabelProvider {
	
	@FunctionalInterface
    public interface LabelSupplier {
        String getLabel(Object o);
    }
    
    LabelSupplier labelSupplier;

	public ColumnLabelProviderAdapter(LabelSupplier labelSupplier) {
		this.labelSupplier = labelSupplier;
	}

	@Override
	public String getText(Object element) {
		// TODO Auto-generated method stub
		return labelSupplier.getLabel(element);
	}


}
