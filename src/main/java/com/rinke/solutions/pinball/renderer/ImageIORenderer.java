package com.rinke.solutions.pinball.renderer;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;

public class ImageIORenderer extends Renderer {
    
    private static Logger LOG = LoggerFactory.getLogger(ImageIORenderer.class); 
	
    private String pattern = "Image-0x%04X.jpg";
    
	private String getFilename(String name, int frameNo) {
		String filename = name + "/"+ String.format(pattern,
				new Object[] { Integer.valueOf(frameNo) });
		return filename;
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
    			return convertTo4Color(master, dmd);
            } else {
            	return convertToFrame(master, dmd);
            }
            

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Frame convertTo4Color(BufferedImage master, DMD dmd) {
		Map<Integer, Integer> grayCounts = new HashMap<>();
		
		byte[] f1 = new byte[dmd.getFrameSizeInByte()];
		byte[] f2 = new byte[dmd.getFrameSizeInByte()];

		Frame res = new Frame(f1, f2);

		for (int x = 0; x < 128; x++) {
			for (int y = 0; y < 32; y++) {
				int rgb = master.getRGB(x, y);
				int gray = (int) ((0.299f * (rgb >> 24)) + 0.587f
						* ((rgb >> 16) & 0xFF) + 0.114f * ((rgb >> 8) & 0xFF));
				// < 20 -> schwarz
				// >= 20 && < 100 ->low						
				// > 100 <180 - mid
				// > 180 - high
				// ironman 20,100,125
				// WCS 20,50,100
				if (gray > lowThreshold && gray < midThreshold) {
					// set f1
					f1[y*dmd.getBytesPerRow() + x / 8] |= (128 >> (x % 8));
				} else if (gray >= midThreshold && gray < highThreshold) {
					f2[y*dmd.getBytesPerRow() + x / 8] |= (128 >> (x % 8));
				} else if (gray >= highThreshold ) {
					f1[y*dmd.getBytesPerRow() + x / 8] |= (128 >> (x % 8));
					f2[y*dmd.getBytesPerRow() + x / 8] |= (128 >> (x % 8));
				}
				if (!grayCounts.containsKey(gray)) {
					grayCounts.put(gray, 0);
				}

				grayCounts.put(gray, grayCounts.get(gray) + 1);
			}
		}
		LOG.debug("----");
		for (int v : grayCounts.keySet()) {
			LOG.debug("Grauwert " + v + " = "
					+ grayCounts.get(v));
		}
		return res;
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

	public static void main(String[] args) {
		Renderer renderer = new ImageIORenderer();
		String base = "/Users/stefanri/Downloads/";
		DMD dmd = new DMD(128, 32);
		renderer.convert(base + "pin", dmd, 0);
	}


}
