package com.rinke.solutions.databinding;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;

import com.rinke.solutions.pinball.util.ObservableCollection;

@Slf4j
public class DataBinder {
	
	static class WidgetBinding {
		public Field f;
		public WidgetProp widgetProp;
		public String propName;
		public boolean bound;
		public WidgetBinding(Field f, WidgetProp widgetProp, String propName) {
			super();
			this.f = f;
			this.widgetProp = widgetProp;
			this.propName = propName;
		}
		@Override
		public String toString() {
			return String.format("WidgetBinding [f=%s, widgetProp=%s, propName=%s]", f, widgetProp, propName);
		}
	}
	
	public void bind(Object view, Object viewModel) {		
		Class<?> viewClz = view.getClass();
		Map<String, WidgetBinding> viewWidgetBindingMap = new HashMap<>();
		for( Field f : viewClz.getDeclaredFields() ) {
			GuiBinding ano = f.getAnnotation(GuiBinding.class);
			if( f.isAnnotationPresent(GuiBinding.class)) {
				List<WidgetProp> props = new ArrayList<>();
				if( !WidgetProp.NONE.equals(ano.prop())) props.add(ano.prop());
				props.addAll(Arrays.asList(ano.props()));
				for( WidgetProp p : props) {
					// create camel case key as string
					String key = p.prefix + (StringUtils.isEmpty(p.prefix) ? f.getName() : StringUtils.capitalize(f.getName())) + p.postfix;
					// iterate over given propNames
					if( !StringUtils.isEmpty(ano.propName())) key = ano.propName();
					
					WidgetBinding widgetBinding = new WidgetBinding(f, p, key);
					log.error("add binding {}", widgetBinding);
					viewWidgetBindingMap.put(key, widgetBinding);
				}
			}
		}
		Class<?> modelClz = viewModel.getClass();
		//Map<String, WidgetBinding> viewBindingMap = new HashMap<>();
		for( Field f : modelClz.getDeclaredFields() ) {
			if( f.isAnnotationPresent(BindingTarget.class)) {
				if( viewWidgetBindingMap.containsKey(f.getName()) ) {
					WidgetBinding widgetBinding = viewWidgetBindingMap.get(f.getName());
					createBinding( widgetBinding, f, view, viewModel );
					widgetBinding.bound = true;
				} else {
					log.error("could not find property source for {} in view", f.getName());
					throw new RuntimeException("could not find property source for "+f.getName()+" in view");
				}
			}
		}
		// sanity check: everything bound
		for( WidgetBinding wb : viewWidgetBindingMap.values()) {
			if( !wb.bound ) {
				throw new RuntimeException("no binding target for "+wb+" in view model");
			}
		}
	}

	DataBindingContext bindingContext = new DataBindingContext();
	
	private void createBinding(WidgetBinding widgetBinding, Field f, Object view, Object model) {
		IObservableValue observer = null;
		boolean observableCollection = false;
		try {
			widgetBinding.f.setAccessible(true);
			Object widget = widgetBinding.f.get(view);
			if( widget instanceof Control) {
				switch( widgetBinding.widgetProp) {
				case ENABLED:
					observer = WidgetProperties.enabled().observe(widget);
					break;
				case SINGLE_SELECTION:
				case SELECTION:
					observer = WidgetProperties.selection().observe(widget);
					break;
				case LABEL:
					observer = WidgetProperties.text().observe(widget);
					break;
				case MIN:
					observer = WidgetProperties.minimum().observe(widget);
					break;
				case MAX:
					observer = WidgetProperties.maximum().observe(widget);
					break;
				case TEXT:
					observer = WidgetProperties.text(SWT.Modify).observe(widget);
					break;
				default:
					break;
				}
			} //  add ListViewer / TableViewer 
			else if( widget instanceof AbstractListViewer) {
				AbstractListViewer viewer = (AbstractListViewer) widget;
				switch( widgetBinding.widgetProp) {
				case INPUT:
					viewer.setContentProvider(ArrayContentProvider.getInstance());
					Object modelProp = getField(model, widgetBinding.propName);
					if( modelProp instanceof ObservableCollection<?>) {
						((ObservableCollection<?>) modelProp).addObserver((o,a)->{
							viewer.setInput(o);
							viewer.refresh();
							// refresh bound detail as well
						});
						observableCollection = true;
					}
					break;
				case SINGLE_SELECTION:
				case SELECTION:
					observer = ViewerProperties.singleSelection().observe(widget);
					break;
				default:
					break;
				}
			} else if( widget instanceof AbstractTableViewer) {
				AbstractTableViewer viewer = (AbstractTableViewer) widget;
				switch( widgetBinding.widgetProp) {
				case INPUT:
					viewer.setContentProvider(ArrayContentProvider.getInstance());
					Object modelProp = getField(model, widgetBinding.propName);
					if( modelProp instanceof ObservableCollection<?>) {
						((ObservableCollection<?>) modelProp).addObserver((o,a)->{
							viewer.setInput(o);
							viewer.refresh();
						});
						observableCollection = true;
					}
					break;
				case SINGLE_SELECTION:
				case SELECTION:
					observer = ViewerProperties.singleSelection().observe(widget);
					break;
				default:
					break;
				}
			} 

		} catch (IllegalArgumentException | IllegalAccessException e) {
			log.error("error accessing view field {}", widgetBinding, e);
		}
		if(observer!=null) {
			// maybe something more (alternative could be PojoProperties)
			IObservableValue observedValue = BeanProperties.value(widgetBinding.propName).observe(model);
			bindingContext.bindValue(observer, observedValue, null, null );
			log.info("created binding for {} <-> {}.{}", widgetBinding, model.getClass().getSimpleName(), f.getName());
		} else {
			if( !observableCollection )
				throw new RuntimeException("couldn't create observer for " + widgetBinding.toString());
		}
			
	}

	private Object getField(Object model, String propName) {
		try {
			Field field = model.getClass().getField(propName);
			field.setAccessible(true);
			return field.get(model);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
		}
		return null;
	}

}
