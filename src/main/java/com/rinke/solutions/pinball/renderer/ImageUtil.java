package com.rinke.solutions.pinball.renderer;

import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;


@Slf4j
public class ImageUtil {
	
	static int lowThreshold = 50;
	static int midThreshold = 120;
	static int highThreshold = 200;
	
	/**
	 * Returns the color distance between color1 and color2
	 */
	public static float getPixelDistance(int c1, int c2) {
		int r1 = (c1 >> 16) & 0xFF;
		int g1 = (c1 >> 8) & 0xFF;
		int b1 = (c1 >> 0) & 0xFF;
		int r2 = (c2 >> 16) & 0xFF;
		int g2 = (c2 >> 8) & 0xFF;
		int b2 = (c2 >> 0) & 0xFF;
		return (float) getPixelDistance(r1, g1, b1, r2, g2, b2);
	}

	public static double getPixelDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
		return Math.sqrt(Math.pow(r2 - r1, 2) + Math.pow(g2 - g1, 2) + Math.pow(b2 - b1, 2));
	}
	
	public static void dumpPlane(byte[] plane, int bytesPerLine) {
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < plane.length; i++) {
			int v = plane[i];
			for(int j = 0; j<8;j++) {
				if( ((128 >> j) & v) != 0 ) sb.append('*'); else sb.append('.');
			}
			if( i % bytesPerLine == (bytesPerLine-1) ) {
				System.out.println(sb.toString());
				sb = new StringBuffer();
			}
		}
	}

	/**
	 * Converts an swt based image into an AWT <code>BufferedImage</code>. This
	 * will always return a <code>BufferedImage</code> that is of type
	 * <code>BufferedImage.TYPE_INT_ARGB</code> regardless of the type of swt
	 * image that is passed into the method.
	 * 
	 * @param srcImage
	 *            the {@link org.eclipse.swt.graphics.Image} to be converted to
	 *            a <code>BufferedImage</code>
	 * @return a <code>BufferedImage</code> that represents the same image data
	 *         as the swt <code>Image</code>
	 */
	public static BufferedImage convert(Image srcImage) {

		ImageData imageData = srcImage.getImageData();
		int width = imageData.width;
		int height = imageData.height;
		ImageData maskData = null;
		int alpha[] = new int[1];

		if (imageData.alphaData == null)
			maskData = imageData.getTransparencyMask();

		// now we should have the image data for the bitmap, decompressed in
		// imageData[0].data.
		// Convert that to a Buffered Image.
		BufferedImage image = new BufferedImage(imageData.width,
				imageData.height, BufferedImage.TYPE_INT_ARGB);

		WritableRaster alphaRaster = image.getAlphaRaster();

		// loop over the imagedata and set each pixel in the BufferedImage to
		// the appropriate color.
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				org.eclipse.swt.graphics.RGB color = imageData.palette.getRGB(imageData.getPixel(x, y));
				image.setRGB(x, y, new java.awt.Color(color.red, color.green,
						color.blue).getRGB());

				// check for alpha channel
				if (alphaRaster != null) {
					if (imageData.alphaData != null) {
						alpha[0] = imageData.getAlpha(x, y);
						alphaRaster.setPixel(x, y, alpha);
					} else {
						// check for transparency mask
						if (maskData != null) {
							alpha[0] = maskData.getPixel(x, y) == 0 ? 0 : 255;
							alphaRaster.setPixel(x, y, alpha);
						}
					}
				}
			}
		}

		return image;
	}

	
	public static ImageData convertToSWT(BufferedImage bufferedImage) {
		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) bufferedImage
					.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(),
					colorModel.getGreenMask(), colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(),
					bufferedImage.getHeight(), colorModel.getPixelSize(),
					palette);
			
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[4];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					int pixel = palette.getPixel(new org.eclipse.swt.graphics.RGB(pixelArray[0],
							pixelArray[1], pixelArray[2]));
					data.setPixel(x, y, pixel);
					data.setAlpha(x, y, pixelArray[3]);
				}
			}
			return data;
		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel) bufferedImage
					.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			org.eclipse.swt.graphics.RGB[] rgbs = new org.eclipse.swt.graphics.RGB[size];
			for (int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new org.eclipse.swt.graphics.RGB(reds[i] & 0xFF, greens[i] & 0xFF,
						blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(),
					bufferedImage.getHeight(), colorModel.getPixelSize(),
					palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		}
		return null;
	}

	public static Frame convertToFrameWithPalette(BufferedImage dmdImage, DMD dmd, Palette palette, boolean modifyDMD) {
		Frame res = new Frame();
		int noOfPlanes = 1;
		while( palette.numberOfColors > (1<<noOfPlanes)) noOfPlanes++;
		
		for( int j = 0; j < noOfPlanes ; j++) {
			res.planes.add(new Plane((byte)j, new byte[dmd.getPlaneSize()]));
		}
		boolean hasAlpha = dmdImage.getColorModel().hasAlpha();
		
		// TODO if has alpha also create a mask
		if( hasAlpha ) {
			res.setMask(new Mask(dmd.getPlaneSize()));
		}
		
		Map<Integer,Integer> alphaDist = new HashMap<>();
		
		for (int x = 0; x < Math.min(dmdImage.getWidth(),dmd.getWidth()); x++) {
			for (int y = 0; y < Math.min(dmdImage.getHeight(),dmd.getHeight()); y++) {

				int rgb = dmdImage.getRGB(x, y);
				int idx = findBestColorIndex(rgb, palette);
				int mask = (0b10000000 >> (x%8));
				if( hasAlpha ) {
					int alpha = (rgb>>24)&0xFF;
					updateDist( alphaDist, alpha );
					boolean v = ((rgb>>24)&0xFF) > 128;
					if( v ) {
		    			res.mask.data[y*dmd.getBytesPerRow()+x/8] |= mask;
		    		} else {
		    			res.mask.data[y*dmd.getBytesPerRow()+x/8] &= ~mask;
		    		}
					if( v ) {
						if( modifyDMD ) {
							dmd.setPixel(x, y, idx);
						}
						for( int j = 0; j < noOfPlanes ; j++) {
							if( (idx & (1<<j)) != 0) {
									res.planes.get(j).data[y * dmd.getBytesPerRow() + x / 8] |= mask;
							}
						}
					}
				} else {
					if( modifyDMD ) {
						dmd.setPixel(x, y, idx);
					}
					for( int j = 0; j < noOfPlanes ; j++) {
						if( (idx & (1<<j)) != 0) {
								res.planes.get(j).data[y * dmd.getBytesPerRow() + x / 8] |= mask;
						}
					}

				}
			}
		}
		//log.info("distribution of alpha channel: {}", printDistri(alphaDist));
		return res;
	}
	
	private static String printDistri(Map<Integer, Integer> map) {
		StringBuilder sb= new StringBuilder();
		for( Entry<Integer, Integer> i : map.entrySet()) {
			sb.append("val="); sb.append(i.getKey()); sb.append(", c=");
			sb.append(i.getValue()); sb.append("\n");
		}
		return sb.toString();
	}

	private static void updateDist(Map<Integer, Integer> map, int alpha) {
		Integer count = map.get(alpha);
		if( count != null ) {
			count++;
		} else {
			map.put(alpha, new Integer(1));
		}
	}

	private static int findColorIndex(int rgb, Palette palette) {
		for (int i = 0; i < palette.colors.length; i++) {
			RGB p = palette.colors[i];
			if( p.red == ((rgb >> 16) & 0xFF) && p.green == ((rgb >> 8) & 0xFF) && p.blue == (rgb & 0xFF) ) {
				return i;
			}
			
		}
		return 0;
	}

	private static int findBestColorIndex(int rgb, Palette palette) {
		float min = Float.MAX_VALUE;
		int best = 0;
		for (int i = 0; i < palette.colors.length; i++) {
			RGB p = palette.colors[i];
			float colDelta = getPixelDistance((p.red<<16)|(p.green<<8)|p.blue, rgb);
			if( colDelta < min ) {
				min = colDelta;
				best = i;
			}
			
		}
		return best;
	}
	
	public static Frame convertToFrame(BufferedImage dmdImage, int w, int h, int bitsPerChannel) {
		if( bitsPerChannel > 8 ) bitsPerChannel = 8;
		Frame res = new Frame(bitsPerChannel,w,h);
		int mask =  ~(1<<bitsPerChannel) & 0xFF;

		for (int x = 0; x < dmdImage.getWidth(); x++) {
			for (int y = 0; y < dmdImage.getHeight(); y++) {

				int rgb = dmdImage.getRGB(x, y);

				// reduce color depth to bitsPerChannel bit
				int nrgb = ( rgb >> (8-bitsPerChannel) ) & mask;
				nrgb |= ( ( rgb >> (16-bitsPerChannel) ) & mask ) << bitsPerChannel;
				nrgb |= ( ( rgb >> (24-bitsPerChannel) ) & mask ) << bitsPerChannel*2;
				
				for( int j = 0; j < bitsPerChannel * 3 ; j++) {
					if( (nrgb & (1<<j)) != 0)
						res.planes.get(j).data[y * (w/8) + x / 8] |= (0b10000000 >> (x % 8));
				}
			}
		}
		return res;
	}

	public static Frame convertTo4Color(BufferedImage master, int w, int h) {
		Map<Integer, Integer> grayCounts = new HashMap<>();
		int frameSizeInByte = w*h/8;
		int bytesPerRow = w/8;
		byte[] f1 = new byte[frameSizeInByte];
		byte[] f2 = new byte[frameSizeInByte];

		Frame res = new Frame(f1, f2);

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
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
					f1[y*bytesPerRow + x / 8] |= (w >> (x % 8));
				} else if (gray >= midThreshold && gray < highThreshold) {
					f2[y*bytesPerRow + x / 8] |= (w >> (x % 8));
				} else if (gray >= highThreshold ) {
					f1[y*bytesPerRow + x / 8] |= (w >> (x % 8));
					f2[y*bytesPerRow + x / 8] |= (w >> (x % 8));
				}
				if (!grayCounts.containsKey(gray)) {
					grayCounts.put(gray, 0);
				}

				grayCounts.put(gray, grayCounts.get(gray) + 1);
			}
		}
		log.debug("----");
		for (int v : grayCounts.keySet()) {
			log.debug("Grauwert " + v + " = "
					+ grayCounts.get(v));
		}
		return res;
	}

}
