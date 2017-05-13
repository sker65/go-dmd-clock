
package com.rinke.solutions.beans;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.InputStream;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

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
public class SimpleBeanFactory extends DefaultHandler implements BeanFactory {

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
    
    private PropertyProvider propertyProvider;
    
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
		private Class<?> requiredType;
        
        /**
         * ctor using fields
         * @param setter setter method to call
         * @param value value to set
         * @param ref or bean ref to set
         */
        public SetterCall(Method setter, Object value, String ref, Class<?> requiredType) {
            this.setter = setter;
            this.value = value;
            this.ref = ref;
            this.requiredType = requiredType;
        }
    }
    
    /**
     * bean definition consists of singleton flag, class, optional init method
     * and a list of setter calls for initialization.
     * @author sr
     */
    private class BeanDefinition {
    	String name;
        boolean isSingleton;
        Class<?> clazz;
        Method initMethod;
        List<SetterCall> setter = new ArrayList<SetterCall>();
        String factoryBeanname;
        Method factoryMethod;
        public BeanDefinition(String name, boolean isSingleton, Class<?> clazz, Method initMethod) {
            this.isSingleton = isSingleton;
            this.clazz = clazz;
            this.initMethod = initMethod;
            this.name = name;
        }
		@Override
		public String toString() {
			return String.format("BeanDefinition [name=%s, isSingleton=%s, clazz=%s]", name, isSingleton, clazz.getSimpleName());
		}
		public void setFactoryBeanName(String factoryName) {
			this.factoryBeanname = factoryName;
		}
		public void setFactoryMethod(Method factoryMethod) {
			 this.factoryMethod = factoryMethod;
		}
    }

    /**
     * the factory singleton
     */
    private static BeanFactory theInstance;
    
    /**
     * accessor for the factory
     * @return the factory singleton
     */
    public static BeanFactory getInstance() {
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
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.beans.BeanFactory#getBeansOfType(java.lang.Class)
	 */
    @Override
	public <T> List<T> getBeansOfType(Class<T> clz) {
    	List<T> res = new ArrayList<>();
    	HashSet<String> ids = new HashSet<>();
    	for(Entry<String, Object> item : singletons.entrySet()) {
    		if( clz.isAssignableFrom(item.getValue().getClass())) {
    			res.add((T)item.getValue());
    			ids.add(item.getKey());
    		}
    	}
    	for( Entry<String, BeanDefinition> i : beanDefs.entrySet()) {
    		if( !ids.contains(i.getKey()) && clz.isAssignableFrom(i.getValue().clazz)) {
    			res.add((T) getBean(i.getKey()));
    		}
    	}
    	log.debug("getBeanOfType {} -> {}", clz, res);
    	return res;
    }

    public SimpleBeanFactory() {
		super();
	}
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.beans.BeanFactory#scanPackages(java.lang.String)
	 */
    @Override
	public void scanPackages(String pkg) {
    	log.debug("scanning package {}", pkg);
    	Reflections clz = new Reflections(pkg);
    	Set<Class<?>> beans = clz.getTypesAnnotatedWith(Bean.class);
    	for(Class<?> c : beans ) {
    		String simpleName = getBeannameFromClass(c);
    		Bean can = c.getAnnotation(Bean.class);
    		if( !StringUtils.isEmpty(can.name()) ) simpleName = can.name();
    		if( simpleName.length()>0) {
    			log.debug("found bean {}", simpleName);
    			beanDefs.put(simpleName, buildDefs(c, true));
        		// scan for bean factory methods
        		for( Method f : c.getMethods() ) {
        			if( f.isAnnotationPresent(Bean.class) ) {
        				Bean an = f.getAnnotation(Bean.class);
        				// this method creates a bean and should be called, when getBean tries to create an instance
        				String beanName = getBeannameFromClass(f.getReturnType());//getBeannameFromMethodName(f.getName());
        				if( !StringUtils.isEmpty(an.name()) ) beanName = an.name();
        				beanDefs.put(beanName, buildFactoryDef(f,simpleName, an.scope()));
        			}
        		}
    		}
    	}
    	log.info("scan package {} finished: {}", pkg, toString());
    }

	private BeanDefinition buildFactoryDef(Method f, String factoryName, Scope scope) {
		// get type from method
		Class<?> returnType = f.getReturnType();
		BeanDefinition def = buildDefs(returnType, scope.equals(Scope.SINGLETON));
		def.setFactoryBeanName(factoryName);
		def.setFactoryMethod(f);
		return def;
	}

	private String getBeannameFromMethodName(String name) {
		if( name.startsWith("get") ) name = name.substring(3);
		if( name.startsWith("create") ) name = name.substring(6);
		return StringUtils.uncapitalize(name);
	}

	private BeanDefinition buildDefs(Class<?> c, boolean singleton) {
		BeanDefinition def = null;
		Method init = null;
		try {
			init = c.getDeclaredMethod("init");
		} catch (NoSuchMethodException | SecurityException e1) {
		}
		try {
			Bean ba = c.getAnnotation(Bean.class);
			boolean isSingleton = ba!=null ? ba.scope().equals(Scope.SINGLETON) : singleton;
			def = new BeanDefinition(getBeannameFromClass(c), isSingleton, c, init);
			for(Field f: c.getDeclaredFields() ) {
				if(f.isAnnotationPresent(Autowired.class)) {
					def.setter.add(new SetterCall(null, null, f.getName(), f.getType()));
				} else if( f.isAnnotationPresent(Value.class)) {
					Object val = getValueForField(f);
					if( val != null ) def.setter.add(new SetterCall(null, val, f.getName(), f.getType()));
				}
			}
		} catch (SecurityException e) {
			log.error("error scanning bean",e);
		}
		return def;
	}

	private Object getValueForField(Field f) {
		Value valAnno = f.getAnnotation(Value.class);
		Object res = null;
		String key = f.getName();
		if( !StringUtils.isEmpty(valAnno.key()) ) key = valAnno.key();
		String val;
		if( propertyProvider != null ) {
			val = propertyProvider.getProperty(key);
			if( val != null ) res = toObject(val,f.getType());
			else if( !StringUtils.isEmpty(valAnno.defaultValue()) ) res = toObject( valAnno.defaultValue(), f.getType());
		}
		log.debug("retrieving value for field: {} -> {}", f.getName(), res); 
		return res;
	}

	public static Object toObject(String value, Class<?> clazz ) {
	    if( Boolean.class == clazz || Boolean.TYPE == clazz) return Boolean.parseBoolean( value );
	    if( Byte.class == clazz || Byte.TYPE == clazz) return Byte.parseByte( value );
	    if( Short.class == clazz || Short.TYPE == clazz) return Short.parseShort( value );
	    if( Integer.class == clazz || Integer.TYPE == clazz) return Integer.parseInt( value );
	    if( Long.class == clazz || Long.TYPE == clazz) return Long.parseLong( value );
	    if( Float.class == clazz || Float.TYPE == clazz) return Float.parseFloat( value );
	    if( Double.class == clazz || Double.TYPE == clazz) return Double.parseDouble( value );
	    return value;
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
                beanDefs.put(aktbean, new BeanDefinition(aktbean,"true".equals(attributes.getValue("singleton")),
                        clazz,initMethod));
                
            } else if (qName.equals("property")) {
                String name = attributes.getValue("name");
                PropertyDescriptor pd = new PropertyDescriptor(name, beanDefs.get(aktbean).clazz);
                
                // has it a value
                String value = attributes.getValue("value");
                if( value != null ) {
                    PropertyEditor pe = PropertyEditorManager.findEditor(pd.getPropertyType());
                    pe.setAsText(value);
                    beanDefs.get(aktbean).setter.add(new SetterCall(pd.getWriteMethod(),pe.getValue(),null, pd.getPropertyType()));
                }
                
                // has it a reference (must already defined).
                String ref = attributes.getValue("ref");
                if( ref != null ) {
                    beanDefs.get(aktbean).setter.add(new SetterCall(pd.getWriteMethod(),null,ref, pd.getPropertyType()));
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("exception parsing bean " + aktbean, e);
        }
    }

    // merge get / create
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.beans.BeanFactory#getBean(java.lang.String)
	 */
    @Override
	public Object getBean(String id) {
    	log.debug("getting bean {}",id);
        if( singletons.get(id) != null ) {
            return singletons.get(id);
        } else {
            BeanDefinition def = beanDefs.get(id);
            if( def == null ) throw new RuntimeException("no bean with name '"+id+"' found");
            Object bean = null;
            try {
            	if( def.factoryBeanname != null ) {
            		// create factory bean first
            		Object factory = getBean(def.factoryBeanname);
            		bean = def.factoryMethod.invoke(factory);
            	} else {
            		List<Constructor<?>> ctors = new ArrayList<>(Arrays.asList(def.clazz.getDeclaredConstructors()));
            		Optional<Constructor<?>> defCtor = ctors.stream().filter(c->c.getParameterCount()==0).findFirst();
            		// prefer default ctor (if any)
            		if( defCtor.isPresent() ) {
            			bean = tryCreate(def.clazz, defCtor.get());
            			ctors.remove(defCtor.get());
            		}
            		// if defaultCtor wasn't successful, try the others
            		if( bean == null ) {
    	            	for( Constructor<?> ctor : ctors ) {
    	            		bean = tryCreate(def.clazz, ctor);
    	            		if( bean != null) break;
    	            	}
            		}
            	}
            	if( bean == null ) {
            		throw new RuntimeException("could not create bean: "+id);
            	}
                for (SetterCall call : def.setter) {
                    Object v = call.value != null ? call.value : getBeanByTypeAndName(call.requiredType, false, call.ref);
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
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.beans.BeanFactory#setSingleton(java.lang.String, java.lang.Object)
	 */
    @Override
	public void setSingleton(String name, Object instance) {
    	singletons.put(name, instance);
    	beanDefs.put(name, new BeanDefinition(name,true,instance.getClass(),null));
    }

    private Object tryCreate(Class<?> clazz, Constructor<?> ctor) {
    	log.debug("create {} with {}", clazz.getSimpleName(), ctor);
		try {
			if( ctor.getParameterCount() == 0 ) {
				ctor.setAccessible(true);
				return ctor.newInstance();
			} else {
				List<Object> params = new ArrayList<Object>();
				AnnotatedType[] apt = ctor.getAnnotatedParameterTypes();
				Class<?>[] pt = ctor.getParameterTypes();
				for( int i=0; i<pt.length; i++ ) {
					String desiredName = null;
					Autowired aw = apt[i].getAnnotation(Autowired.class);
					if( aw != null && !StringUtils.isEmpty(aw.name()) ) desiredName = aw.name();
					Object p = getBeanByTypeAndName(pt[i], true, desiredName);
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
    
    /* (non-Javadoc)
	 * @see com.rinke.solutions.beans.BeanFactory#getBeanByType(java.lang.Class)
	 */
    @Override
	public <T> T getBeanByType(Class<T> pt) {
    	return getBeanByTypeAndName(pt, false, null);
    }

	/* (non-Javadoc)
	 * @see com.rinke.solutions.beans.BeanFactory#getBeanByTypeAndName(java.lang.Class, boolean, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getBeanByTypeAndName(Class<T> pt, boolean ignoreError, String name) {
		List<BeanDefinition> candidates = new ArrayList<>();
		for(BeanDefinition item : beanDefs.values()) {
			if( pt.isAssignableFrom(item.clazz)) candidates.add(item);
		}
		
		if( candidates.size()==1 ) return (T) getBean(candidates.get(0).name);
		if( name != null && candidates.size()>1) {
			// filter by provided name to narrow scope
			List<BeanDefinition> reduced = candidates.stream().filter(b->name.equals(b.name)).collect(Collectors.toList());
			if( reduced.size() == 1) return (T) getBean(reduced.get(0).name);
			if( !ignoreError ) throw new RuntimeException("cannot create bean of type "+pt + " candidates = "+reduced);
		}
		if( !ignoreError ) throw new RuntimeException("cannot create bean of type '"+pt+"' no beanDef availbable in "+this.toString());
		return null;
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
    
    private String getBeannameFromClass( Class<?> c) {
    	return StringUtils.uncapitalize(c.getName());
    }

	/* (non-Javadoc)
	 * @see com.rinke.solutions.beans.BeanFactory#setSingleton(java.lang.Object)
	 */
	@Override
	public void setSingleton(Object o) {
		setSingleton(getBeannameFromClass(o.getClass()), o);
	}

	/* (non-Javadoc)
	 * @see com.rinke.solutions.beans.BeanFactory#inject(java.lang.Object)
	 */
	@Override
	public void inject(Object o) {
		for( Field f : o.getClass().getDeclaredFields()) {
			if( f.isAnnotationPresent(Autowired.class)) {
				Autowired aw = f.getAnnotation(Autowired.class);
				String desiredName = f.getName();
				if( !StringUtils.isEmpty(aw.name()) ) desiredName = aw.name();
				f.setAccessible(true);
				Object val = getBeanByTypeAndName(f.getType(), false, desiredName);
				if( val == null) throw new RuntimeException("no candidate for field "+f.getName());
				try {
					f.set(o, val);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("error injecting field "+f.getName());
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.rinke.solutions.beans.BeanFactory#setValueProvider(com.rinke.solutions.beans.SimpleBeanFactory.PropertyProvider)
	 */
	@Override
	public void setValueProvider(PropertyProvider p) {
		 this.propertyProvider = p;
	}

	@Override
	public <T> T getBeanOfType(Class<T> pt, String name) {
		return getBeanByTypeAndName(pt, false, name);
	}

}