package com.rinke.solutions.pinball.renderer;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.animation.Frame;

// als parameter in der Steuerdatei sollten
// die helligkeits schwellen angebbar sein

public class AnimatedGIFRenderer extends Renderer {
    
    private static Logger LOG = LoggerFactory.getLogger(AnimatedGIFRenderer.class); 
	
	protected void readImage(String filename, DMD dmd) {
		
		String[] imageatt = new String[]{
                "imageLeftPosition",
                "imageTopPosition",
                "imageWidth",
                "imageHeight"
            };

		try {
			ImageReader reader = (ImageReader) ImageIO
					.getImageReadersByFormatName("gif").next();

			ImageInputStream ciis = ImageIO
					.createImageInputStream(new FileInputStream(filename));

			reader.setInput(ciis, false);

			BufferedImage master = null;

			int noi = reader.getNumImages(true);
			LOG.debug("found " + noi + " images");
			this.maxFrame = noi;
			
			Map<Integer, Integer> grayCounts = new HashMap<>();
			int frameNo = 0;
			while (frameNo < noi) {
				
				byte[] f1 = new byte[dmd.getFrameSizeInByte()];
				byte[] f2 = new byte[dmd.getFrameSizeInByte()];

				Frame res = new Frame(dmd.getWidth(), dmd.getHeight(), f1, f2);

				BufferedImage image = reader.read(frameNo);
	            IIOMetadata metadata = reader.getImageMetadata(frameNo);

	            Node tree = metadata.getAsTree("javax_imageio_gif_image_1.0");

	            NodeList children = tree.getChildNodes();

	            for (int j = 0; j < children.getLength(); j++) {

	                Node nodeItem = children.item(j);

	                if(nodeItem.getNodeName().equals("ImageDescriptor")){

	                    Map<String, Integer> imageAttr = new HashMap<String, Integer>();

	                    for (int k = 0; k < imageatt.length; k++) {

	                        NamedNodeMap attr = nodeItem.getAttributes();

	                        Node attnode = attr.getNamedItem(imageatt[k]);

	                        imageAttr.put(imageatt[k], Integer.valueOf(attnode.getNodeValue()));

	                    }


	                    if(frameNo==0){
	                        master = new BufferedImage(imageAttr.get("imageWidth"), imageAttr.get("imageHeight"), BufferedImage.TYPE_INT_ARGB);

	                    }
	                    master.getGraphics().drawImage(image, imageAttr.get("imageLeftPosition"), imageAttr.get("imageTopPosition"), null);


	                }
	            }
	            
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
				frames.add(res);
				frameNo++;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
