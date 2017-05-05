
package com.rinke.solutions.beans;

import static org.hamcrest.Matchers.stringContainsInOrder;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Display;
import org.reflections.Reflections;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
* simple bean factory.
* @author sr
*/
@Slf4j
public class SimpleBeanFactory extends DefaultHandler {

    /**
     * stores singletons
     */
    private Map<String, Object> singletons = new HashMap<String, Object>();
    /**
     * stores bean definitions
     */
    private Map<String, BeanDefinition> beanDefs = new HashMap<String, BeanDefinition>();

    /**
     *  state of parser
     */
    private String aktbean;
    
    /**
     * part of bean definition: a setter call for init. 
     * @author sr
     */
    private class SetterCall {
        /**
         * setter to call.
         */
        Method setter;
        /**
         * immediate value to set.
         */
        Object value;
        /**
         * alternatively: bean ref to set.
         */
        String ref;
        /**
         * ctor using fields
         * @param setter setter method to call
         * @param value value to set
         * @param ref or bean ref to set
         */
        public SetterCall(Method setter, Object value, String ref) {
            this.setter = setter;
            this.value = value;
            this.ref = ref;
        }
    }
    
    /**
     * bean definition consists of singleton flag, class, optional init method
     * and a list of setter calls for initialization.
     * @author sr
     */
    private class BeanDefinition {
        boolean isSingleton;
        Class<?> clazz;
        Method initMethod;
        List<SetterCall> setter = new ArrayList<SetterCall>();
        public BeanDefinition(boolean isSingleton, Class<?> clazz, Method initMethod) {
            this.isSingleton = isSingleton;
            this.clazz = clazz;
            this.initMethod = initMethod;
        }
    }

    /**
     * the factory singleton
     */
    private static SimpleBeanFactory theInstance;
    
    /**
     * accessor for the factory
     * @return the factory singleton
     */
    public static SimpleBeanFactory getInstance() {
        if( theInstance == null ) {
            theInstance = new SimpleBeanFactory(SimpleBeanFactory.class.getResourceAsStream("/context.xml"));
        }
        return theInstance;
    }
    
    public SimpleBeanFactory(String resource) {
    	this(SimpleBeanFactory.class.getResourceAsStream(resource));
    }
    
    /**
     * ctor using input stream
     * @param is
     */
    public SimpleBeanFactory(InputStream is) {
        parse(is);
    }

    public SimpleBeanFactory() {
		super();
	}
    
    public void scanPackages(String pkg) {
    	Reflections clz = new Reflections(pkg);
    	Set<Class<?>> beans = clz.getTypesAnnotatedWith(Bean.class);
    	for(Class<?> c : beans ) {
    		String simpleName = StringUtils.uncapitalize(c.getSimpleName());
    		if( simpleName.length()>0) {
    			log.info("found bean {}", simpleName);
    			beanDefs.put(simpleName, buildDefs(c));
    		}
    	}
    }

	private BeanDefinition buildDefs(Class<?> c) {
		BeanDefinition def = null;
		Method init = null;
		try {
			init = c.getDeclaredMethod("init");
		} catch (NoSuchMethodException | SecurityException e1) {
		}
		try {
			def = new BeanDefinition(true, c, init);
			for(Field f: c.getDeclaredFields() ) {
				if(f.isAnnotationPresent(Autowired.class)) {
					def.setter.add(new SetterCall(null, null, f.getName()));
				}
			}
		} catch (SecurityException e) {
			log.error("error scanning bean",e);
		}
		return def;
	}

	/**
     * parser handler
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        try {
            if (qName.equals("bean")) {
                aktbean = attributes.getValue("id");
                if( beanDefs.get(aktbean) != null ) {
                    throw new IllegalArgumentException("duplicate bean name " + aktbean);
                }
                String classname = attributes.getValue("class");
                String initMethodName = attributes.getValue("init-method");
                Method initMethod = null;
                if( initMethodName != null ) {
                    initMethod = Class.forName(classname).getMethod(initMethodName,new Class[]{});
                }
                Class<?> clazz = Class.forName(classname);
                beanDefs.put(aktbean, new BeanDefinition("true".equals(attributes.getValue("singleton")),
                        clazz,initMethod));
                
            } else if (qName.equals("property")) {
                String name = attributes.getValue("name");
                PropertyDescriptor pd = new PropertyDescriptor(name, beanDefs.get(aktbean).clazz);
                
                // has it a value
                String value = attributes.getValue("value");
                if( value != null ) {
                    PropertyEditor pe = PropertyEditorManager.findEditor(pd.getPropertyType());
                    pe.setAsText(value);
                    beanDefs.get(aktbean).setter.add(new SetterCall(pd.getWriteMethod(),pe.getValue(),null));
                }
                
                // has it a reference (must already defined).
                String ref = attributes.getValue("ref");
                if( ref != null ) {
                    beanDefs.get(aktbean).setter.add(new SetterCall(pd.getWriteMethod(),null,ref));
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("exception parsing bean " + aktbean, e);
        }
    }

    // merge get / create
    
    public Object getBean(String id) {
    	log.info("getting bean {}",id);
        if( singletons.get(id) != null ) {
            return singletons.get(id);
        } else {
            BeanDefinition def = beanDefs.get(id);
            if( def == null ) throw new RuntimeException("no bean with name '"+id+"' found");
            Object bean = null;
            try {
            	for( Constructor<?> ctor : def.clazz.getDeclaredConstructors() ) {
            		bean = tryCreate(def.clazz, ctor);
            		if( bean != null) break;
            	}
            	if( bean == null ) {
            		throw new RuntimeException("could not create bean: "+id);
            	}
                for (SetterCall call : def.setter) {
                    Object v = call.value != null ? call.value : getBean( call.ref );
                    if( call.setter != null ) {
                    	call.setter.invoke(bean,new Object[] {v});
                    } else {
                    	// inject field
                    	Field field = def.clazz.getDeclaredField(call.ref);
                    	field.setAccessible(true);
                    	field.set(bean, v);
                    }
                }
    
                if( def.initMethod != null ) {
                    def.initMethod.invoke(bean,new Object[]{});
                }
                
            } catch (Exception e) {
                throw new IllegalArgumentException("error creating bean "+ id,e);
            }
            if( def.isSingleton ) {
                singletons.put(id,bean);
            }
            return bean;
        }
    }
    
    public void setSingleton(String name, Object instance) {
    	singletons.put(name, instance);
    	beanDefs.put(name, new BeanDefinition(true,instance.getClass(),null));
    }

    private Object tryCreate(Class<?> clazz, Constructor<?> ctor) {
    	log.info("try to create {} with {}", clazz.getSimpleName(), ctor);
		try {
			if( ctor.getParameterCount() == 0 ) {
				ctor.setAccessible(true);
				return ctor.newInstance();
			} else {
				List<Object> params = new ArrayList<Object>();
				for( Class<?> pt : ctor.getParameterTypes() ) {
					Object p = getBeanByType(pt);
					if( p == null ) return null;
					params.add(p);
				}
				ctor.setAccessible(true);
				return ctor.newInstance(params.toArray(new Object[params.size()]));
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("error creating bean");
		}
		return null;
	}

	private Object getBeanByType(Class<?> pt) {
		for(Entry<String, BeanDefinition> i : beanDefs.entrySet()) {
			if( pt.isAssignableFrom(i.getValue().clazz)) return getBean(i.getKey());
		}
		throw new RuntimeException("cannot create bean of type "+pt);
	}

	/**
     * parse config and init factory instance.
     * @param is
     */
    private void parse(InputStream is) {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(is, this);
        } catch (Exception e) {
            throw new IllegalArgumentException("parser error",e);
        }

    }

	public void setSingleton(Object o) {
		setSingleton(StringUtils.uncapitalize(o.getClass().getSimpleName()), o);
	}

	public void inject(Object o) {
		for( Field f : o.getClass().getDeclaredFields()) {
			if( f.isAnnotationPresent(Autowired.class)) {
				f.setAccessible(true);
				Object val = getBeanByType(f.getType());
				if( val == null) throw new RuntimeException("no candidate for field "+f.getName());
				try {
					f.set(o, val);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("error injecting field "+f.getName());
				}
			}
		}
	}

}