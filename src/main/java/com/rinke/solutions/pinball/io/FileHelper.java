package com.rinke.solutions.pinball.io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;

import lombok.extern.slf4j.Slf4j;


import com.rinke.solutions.pinball.model.Format;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameSeq;
import com.rinke.solutions.pinball.model.Model;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.model.Scene;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.mapper.Mapper;

@Slf4j
public class FileHelper {
	
    private XStream xstream;
    private XStream jstream;
    private XStream bstream;
    BinaryStreamDriver driver;

    public FileHelper() {
		setupXStream();
	}

    @SuppressWarnings("rawtypes")
    public static class DefaultConstructorConverter extends ReflectionConverter {
        public DefaultConstructorConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
            super(mapper, reflectionProvider);
        }
        
		@Override
        public boolean canConvert(Class clazz) {
			if( ! clazz.getName().startsWith("com.rinke.solutions.pinball.")) return false;
            for (Constructor c : clazz.getConstructors()) {
                if (c.getParameterTypes().length == 0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected Object instantiateNewInstance(HierarchicalStreamReader reader, UnmarshallingContext context) {
            try {
            	Class clazz = mapper.realClass(reader.getNodeName());
                return clazz.newInstance();
            } catch (Exception e) {
                throw new ConversionException("Could not create instance of class " + reader.getNodeName(), e);
            }
        }
    }
    
    private void setupXStream() {
        xstream = new XStream();
        xstream.registerConverter(new DefaultConstructorConverter(xstream.getMapper(),
        		xstream.getReflectionProvider()));
        jstream = new XStream(new JettisonMappedXmlDriver());
        jstream = new XStream(new JettisonMappedXmlDriver());
        
		driver = new BinaryStreamDriver();
		bstream = new XStream(driver);
        bstream.alias("rgb", RGB.class);
        bstream.alias("palette", Palette.class);
        bstream.alias("project", Project.class);
        bstream.alias("palMapping", PalMapping.class);
        bstream.alias("scene", Scene.class);
        bstream.alias("frameSeq", FrameSeq.class);
        bstream.alias("frame", Frame.class);
        
        bstream.setMode(XStream.NO_REFERENCES);
        
        xstream.alias("rgb", RGB.class);
        xstream.alias("palette", Palette.class);
        xstream.alias("project", Project.class);
        xstream.alias("palMapping", PalMapping.class);
        xstream.alias("scene", Scene.class);
        xstream.alias("frameSeq", FrameSeq.class);
        xstream.alias("frame", Frame.class);
        
        xstream.setMode(XStream.NO_REFERENCES);
        jstream.alias("rgb", RGB.class);
        jstream.alias("palette", Palette.class);
        jstream.alias("project", Project.class);
        jstream.alias("palMapping", PalMapping.class);
        jstream.alias("scene", Scene.class);
        jstream.alias("frameSeq", FrameSeq.class);
        jstream.alias("frame", Frame.class);
        
        jstream.setMode(XStream.NO_REFERENCES);
    }
    
    public void storeObject(Model obj,  String filename) {
    	try( OutputStream out = new FileOutputStream(filename)) {
    		storeObject(obj, out, Format.byFilename(filename));
    	} catch( IOException e) {
    	    log.error("error on storing "+filename, e);
    	    throw new RuntimeException("error on storing "+filename,e);
    	}
    }
    
    public void storeObject(Model obj,  OutputStream out, Format format) throws IOException {
            HierarchicalStreamWriter writer = null;
            switch (format) {
            case XML:
                xstream.toXML(obj, out);
                break;
            case JSON:
                jstream.toXML(obj, out);
                break;
            case BIN:
                writer = driver.createWriter(out);
                bstream.marshal(obj, writer);
                break;
            default:
                throw new RuntimeException("unsupported filetype ");
            }
            if(writer!=null) writer.close(); 
            else out.close();
    }
    
    public Object loadObject(String filename) {
        Object res = null;
    	try ( InputStream in = new FileInputStream(filename) ) {
            HierarchicalStreamReader reader = null;
            
            switch (Format.byFilename(filename)) {
            case XML:
                return xstream.fromXML(in);
            case JSON:
                return jstream.fromXML(in);
            case BIN:
                reader = driver.createReader(in);
                res = bstream.unmarshal(reader, null);
                break;

            default:
                throw new RuntimeException("unsupported filetype / extension " +filename);
            }
    	} catch( IOException e2) {
    	    log.error("error on load "+filename,e2);
    	    throw new RuntimeException("error on load "+filename, e2);
    	}
    	return res;
    }

}
