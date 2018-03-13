package com.rinke.solutions.pinball.view.model;

import java.util.Observable;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.rinke.solutions.beans.Bean;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@Bean
public class XStreamUtil {
	
	private XStream xStream;

	public void init() {
		this.xStream = new XStream();
		xStream.omitField(Observable.class, "obs");
		class SWTObservablesConverter implements Converter {

	        public boolean canConvert(Class clazz) {
	                return clazz.getName().startsWith("org.eclipse.jface.databinding.swt.");
	        }

			@Override
			public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
				
			}

			@Override
			public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
				return null;
			}
		}
		xStream.registerConverter(new SWTObservablesConverter());
		xStream.omitField(ViewModel.class, "previewPalettes");
		//xStream.omitField(TypedLabel.class, "change");
		
		Reflections reflections = new Reflections((new ConfigurationBuilder()
	     .setUrls(ClasspathHelper.forPackage("com.rinke.solutions"))
	     .setScanners(new SubTypesScanner(false)))); //"com.rinke.solutions");
		
		for( Class<?> type : reflections.getSubTypesOf(Object.class) ) {
			String name = type.getName();
			if( !name.contains("$")) {
				xStream.alias(name.substring("com.rinke.solutions.".length()), type);
			}
		}
	}

	public String toXML(Object o) {
		return xStream.toXML(o);
	}

}