package com.rinke.solutions.beans;

import java.util.List;

public interface BeanFactory {

	public <T> List<T> getBeansOfType(Class<T> clz);

	public void scanPackages(String pkg);

	public Object getBean(String id);

	public void setSingleton(String name, Object instance);

	public <T> T getBeanByType(Class<T> pt);

	public <T> T getBeanByTypeAndName(Class<T> pt, boolean ignoreError, String name);

	public void setSingleton(Object o);

	public void inject(Object o);

	public void setValueProvider(PropertyProvider p);
	
    public interface PropertyProvider {
    	String getProperty(String key);
    }

}