package com.rinke.solutions.pinball;

import org.eclipse.jface.viewers.LabelProvider;

/**
 * adapter for jface label providers to use them with lamba's extracting text for labels from objects
 * @author stefanri
 */
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
