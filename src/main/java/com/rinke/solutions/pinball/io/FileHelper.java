package com.rinke.solutions.pinball.io;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.swt.graphics.RGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.model.Format;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.NamedFrameSeq;
import com.rinke.solutions.pinball.model.Model;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.model.Scene;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

public class FileHelper {
	
    private static Logger LOG = LoggerFactory.getLogger(FileHelper.class); 
	
    private XStream xstream;
    private XStream jstream;
    private XStream bstream;
    BinaryStreamDriver driver;

    public FileHelper() {
		setupXStream();
	}
	
    private void setupXStream() {
        xstream = new XStream();
        jstream = new XStream(new JettisonMappedXmlDriver());
        jstream = new XStream(new JettisonMappedXmlDriver());
        
		driver = new BinaryStreamDriver();
		bstream = new XStream(driver);
        bstream.alias("rgb", RGB.class);
        bstream.alias("palette", Palette.class);
        bstream.alias("project", Project.class);
        bstream.alias("palMapping", PalMapping.class);
        bstream.alias("scene", Scene.class);
        bstream.alias("frameSeq", NamedFrameSeq.class);
        bstream.alias("frame", Frame.class);
        
        bstream.setMode(XStream.NO_REFERENCES);
        
        xstream.alias("rgb", RGB.class);
        xstream.alias("palette", Palette.class);
        xstream.alias("project", Project.class);
        xstream.alias("palMapping", PalMapping.class);
        xstream.alias("scene", Scene.class);
        xstream.alias("frameSeq", NamedFrameSeq.class);
        xstream.alias("frame", Frame.class);
        
        xstream.setMode(XStream.NO_REFERENCES);
        jstream.alias("rgb", RGB.class);
        jstream.alias("palette", Palette.class);
        jstream.alias("project", Project.class);
        jstream.alias("palMapping", PalMapping.class);
        jstream.alias("scene", Scene.class);
        jstream.alias("frameSeq", NamedFrameSeq.class);
        jstream.alias("frame", Frame.class);
        
        jstream.setMode(XStream.NO_REFERENCES);
    }
    
    public void storeObject(Model obj,  String filename) {
    	try( OutputStream out = new FileOutputStream(filename)) {
            HierarchicalStreamWriter writer = null;
            switch (Format.byFilename(filename)) {
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
            case DAT:
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(filename));
                obj.writeTo(dos);
                dos.close();
                break;
                
            default:
                throw new RuntimeException("unsupported filetype / extension " +filename);
            }
            if(writer!=null) writer.close(); else out.close();
    	} catch( IOException e) {
    	    LOG.error("error on storing "+filename, e);
    	    throw new RuntimeException("error on storing "+filename,e);
    	}
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
    	    LOG.error("error on load "+filename,e2);
    	    throw new RuntimeException("error on load "+filename, e2);
    	}
    	return res;
    }

}
