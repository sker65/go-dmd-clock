package com.rinke.solutions.pinball.renderer;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;

public class ImageIORenderer extends Renderer {
    
    private static Logger LOG = LoggerFactory.getLogger(ImageIORenderer.class); 
	
    private String pattern = "Image-0x%04X.jpg";
    
	public ImageIORenderer(String pat) {
		if( pat != null ) this.pattern = pat;
	}

	private String getFilename(String name, int frameNo) {
		String filename = name + "/"+ String.format(pattern,
				new Object[] { Integer.valueOf(frameNo) });
		return filename;
	}
	
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public Frame convert(String name, DMD dmd, int frameNo, Shell shell) {
		return readImage( name, dmd, frameNo);
	}
	
	@Override
	public Frame convert(String name, DMD dmd, int frameNo) {
		return readImage( name, dmd, frameNo);
	}
	
	private Frame readImage(String name, DMD dmd, int frameNo) {

		String filename = getFilename(name, frameNo);
		String extension = getExtension(filename);
		try {
			ImageReader reader = (ImageReader) ImageIO
					.getImageReadersByFormatName(extension).next();

			ImageInputStream ciis = ImageIO
					.createImageInputStream(new FileInputStream(filename));

			reader.setInput(ciis, false);

			BufferedImage master = null;

			int noi = reader.getNumImages(true);
			LOG.debug("found " + noi + " images");
			this.maxFrame = noi;
			
			BufferedImage image = reader.read(0);
            master = new BufferedImage(image.getWidth(),image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            master.getGraphics().drawImage(image, 0, 0, null);
            
            int planes = getInt("planes", 2);
            
            if( planes == 2 ) {
    			return ImageUtil.convertTo4Color(master, dmd.getWidth(), dmd.getHeight());
            } else {
            	return ImageUtil.convertToFrame(master, dmd.getWidth(), dmd.getHeight());
            }
            

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private int getInt(String propName, int defVal) {
		return Integer.parseInt(
			getProps().getProperty(propName, String.valueOf(defVal))
			);
	}


	private String getExtension(String name) {
		int p = name.lastIndexOf(".");
		return p>0?name.substring(p+1):name;
	}


}
