package com.rinke.solutions.pinball;

import org.eclipse.jface.viewers.LabelProvider;

/**
 * adapter for jface label providers to use them with lamba's extracting text for labels from objects
 * @author stefanri
 */
public class LabelProviderAdapter<T> extends LabelProvider {
    
    @FunctionalInterface
    public interface LabelSupplier<T> {
        String getLabel(T o);
    }
    
    LabelSupplier<T> labelSupplier;

    public LabelProviderAdapter(LabelSupplier<T> labelSupplier) {
        super();
        this.labelSupplier = labelSupplier;
    }

    @SuppressWarnings("unchecked")
	@Override
    public String getText(Object element) {
        return labelSupplier.getLabel((T)element);
    }
    
}
