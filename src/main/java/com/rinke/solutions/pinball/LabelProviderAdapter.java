package com.rinke.solutions.pinball;

import org.eclipse.jface.viewers.LabelProvider;

public class LabelProviderAdapter extends LabelProvider {
    
    @FunctionalInterface
    public interface LabelSupplier {
        String getLabel(Object o);
    }
    
    LabelSupplier labelSupplier;

    public LabelProviderAdapter(LabelSupplier labelSupplier) {
        super();
        this.labelSupplier = labelSupplier;
    }

    @Override
    public String getText(Object element) {
        return labelSupplier.getLabel(element);
    }
    
}
