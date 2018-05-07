package com.rinke.solutions.pinball.renderer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

import com.google.common.collect.Sets;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;

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
		
		DmdSize size = Integer.parseInt(props.getProperty("width", "128"))==128 ? DmdSize.Size128x32 : DmdSize.Size192x64;
		
		try {
			ImageReader reader = null;//(ImageReader) ImageIO.getImageReadersByFormatName("gif").next();

			ImageInputStream ciis = ImageIO.createImageInputStream(new FileInputStream(filename));

			reader = new PatchedGIFImageReader(null);
			reader.setInput(ciis, false);

			BufferedImage master = null;

			int noi = reader.getNumImages(true);
			LOG.info("found " + noi + " images");
			this.maxFrame = noi;
			
			Map<Integer, Integer> grayCounts = new HashMap<>();
			
			IIOMetadata streamMetadata = reader.getStreamMetadata("javax_imageio_1.0", Sets.newHashSet("Palette") );
			NodeList snodes = streamMetadata.getAsTree("javax_imageio_1.0").getChildNodes();
			snodes = find("Chroma/Palette", snodes);
			
			palette = buildPaletteFromNodes(snodes);
			//System.out.println(palette);
			int frameNo = 0;
			while (frameNo < noi) {
				
				BufferedImage image;
				IIOMetadata metadata;
				try {
					image = reader.read(frameNo);
					metadata = reader.getImageMetadata(frameNo);
					
				} catch( RuntimeException e) {
					LOG.error("reading img resulted in error", e);
//					frames.add(new Frame(f1, f2));
					frameNo++;
					continue;
				}
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
	            BufferedImage toScan = master;
	            if(  props.containsKey("crop.x") ||props.containsKey("crop.y") ||props.containsKey("crop.w") ||props.containsKey("crop.h")  ) {
	            	toScan = cropImage(toScan, 
	            			Integer.parseInt(props.getProperty("crop.x", "0")),
	            			Integer.parseInt(props.getProperty("crop.y", "0")),
	            			Integer.parseInt(props.getProperty("crop.w", "0")),
	            			Integer.parseInt(props.getProperty("crop.h", "0"))
	            			);
	            }
	            
	            // resize on demand
	            if( toScan.getWidth() > size.width || toScan.getHeight() > size.height ) {
	            	toScan = resize(toScan, size.width,size.height);
	            }
	            
/*				for (int x = 0; x < 128; x++) {
					for (int y = 0; y < 32; y++) {
						int rgb = toScan.getRGB(x, y) & 0X00FFFFFF; // cut alpha
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
				frames.add(new Frame(f1, f2));*/

	            if( palette.numberOfColors < 256 ) {
	            	frames.add(ImageUtil.convertToFrameWithPalette(toScan, dmd, palette, false));
	            } else {
	            	frames.add(ImageUtil.convertToFrame(toScan, dmd.getWidth(), dmd.getHeight()));
	            }

				frameNo++;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private Palette buildPaletteFromNodes(NodeList snodes) {
		List<RGB> rgbs = new ArrayList<>();
		for (int j = 0; j < snodes.getLength(); j++) {
			Node item = snodes.item(j);
			if( item.getNodeName().equals("PaletteEntry")) {
				NamedNodeMap attr = item.getAttributes();
				RGB rgb = new RGB(
						Integer.parseInt(attr.getNamedItem("red").getNodeValue()),
						Integer.parseInt(attr.getNamedItem("green").getNodeValue()),
						Integer.parseInt(attr.getNamedItem("blue").getNodeValue()));
				rgbs.add(rgb);
			}
		}
		return new Palette(rgbs.toArray(new RGB[rgbs.size()]));
	}

	private NodeList find(String path, NodeList snodes) {
		String[] pathElement = path.split("/");
		if( pathElement.length>=1) {
			for( int j = 0; j < snodes.getLength(); j++) {
				if( snodes.item(j).getNodeName().equals(pathElement[0])) {
					int i = path.indexOf('/');
					return i>0 ? find(path.substring(path.indexOf('/')+1), snodes.item(j).getChildNodes() ) : snodes.item(j).getChildNodes();
				}
			}
		}
		return snodes;
	}

	private BufferedImage cropImage(BufferedImage src, int x, int y, int w, int h) {
		w = Math.min(w, src.getWidth()-x);
		h = Math.min(h, src.getHeight()-y);
		return src.getSubimage(x, y, w, h);
	}
	
	public BufferedImage resize(BufferedImage img, int newW, int newH) { 
	    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
	    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2d = dimg.createGraphics();
	    g2d.drawImage(tmp, 0, 0, null);
	    g2d.dispose();

	    return dimg;
	}  

}
