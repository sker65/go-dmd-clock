package com.rinke.solutions.pinball.renderer;

import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.renderer.video.FFmpegFrameGrabber;
import com.rinke.solutions.pinball.renderer.video.Java2DFrameConverter;

@Slf4j
public class VideoCapRenderer extends Renderer {

	private static Logger LOG = LoggerFactory.getLogger(VideoCapRenderer.class);
	private int skip = 0;

	public VideoCapRenderer(int start, int end) {
	}

	protected void readImage(String name, DMD dmd) {

		Java2DFrameConverter converter = new Java2DFrameConverter();
		FFmpegFrameGrabber g = new FFmpegFrameGrabber(name);
		int w = dmd.getWidth();
		int h = dmd.getHeight();
		skip = getInt("start", 0);
		int end = getInt("end",0);
		AffineTransformOp op = null;
		if( getProps().containsKey("scalex") || getProps().containsKey("scaley")) {
			AffineTransform tx = new AffineTransform(); 
			double sx = getDouble("scalex", 1.0);
			double sy = getDouble("scaley", 1.0);
			log.info("scaling image by {},{}",sx,sy);
			tx.scale(sx, sy);
			op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		}
		try {
			g.start();
			for (int i = 0; i <end+1; i++) {
				Object frame;
				do {
					frame = g.grab();
					if( frame == null ) break;
				} while( !g.containsData(frame) );

				if( frame == null ) break;
				if( i < skip-1 ) continue;
				
				BufferedImage image = converter.convert(frame);
				//int sh = image.getHeight();
				//int sw = image.getWidth();
				
				if( op != null ) image = op.filter(image, null);
				// extract clipping from props
				
				BufferedImage dmdImage = new BufferedImage(w,
						h, BufferedImage.TYPE_INT_RGB);

				Graphics graphics = dmdImage.createGraphics();
				graphics.drawImage(image.getSubimage(getInt("clipx",0), getInt("clipy",0), getInt("clipw",128), getInt("cliph",32)), 0, 0, w,
						h, null);
				graphics.dispose();
				
//				try {
//					ImageIO.write(dmdImage, "PNG",
//							new File(String.format("/Users/stefanri/tmp/%d-%b-cap%d.png", bufLen, kf, i)));
//				} catch (IOException e) {
//					e.printStackTrace();
//				}

				
				 

//				int[][] image2d = convertTo2D(dmdImage);
				// int[] palette = Quantize.quantizeImage(image2d, 255);

				// create 
				Frame res = new Frame();
				for( int j = 0; j < 15 ; j++) {
					res.planes.add(new Plane((byte)j, new byte[dmd.getFrameSizeInByte()]));
				}
				res.timecode = (int)( g.getTimestamp() / 1000 );
				
				for (int x = 0; x < dmd.getWidth(); x++) {
					for (int y = 0; y < dmd.getHeight(); y++) {

						int rgb = dmdImage.getRGB(x, y);
						
						// reduce color depth to 15 bit
						int nrgb = ( rgb >> 3 ) & 0x1F;
						nrgb |= ( ( rgb >> 11 ) & 0x1F ) << 5;
						nrgb |= ( ( rgb >> 19 ) & 0x1F ) << 10;
						
						for( int j = 0; j < 15 ; j++) {
							if( (nrgb & (1<<j)) != 0)
								res.planes.get(j).plane[y * dmd.getBytesPerRow() + x / 8] |= (128 >> (x % 8));
						}

					}
				}
				frames.add(res);
			} // stop cap
			g.stop();
		} catch (Exception e) {
			LOG.error("problems grabbing {}", name, e);
		}
		this.maxFrame = frames.size();
	}

	private double getDouble(String propName, double defValue) {
		return Double.parseDouble(
				getProps().getProperty(propName, String.valueOf(defValue))
				);
	}

	private int getInt(String propName, int defVal) {
		return Integer.parseInt(
			getProps().getProperty(propName, String.valueOf(defVal))
			);
	}

	private int[][] convertTo2D(BufferedImage image) {

		final int width = image.getWidth();
		final int height = image.getHeight();
		int[][] result = new int[height][width];

		DataBuffer dataBuffer = image.getRaster().getDataBuffer();

		if (dataBuffer.getDataType() == DataBuffer.TYPE_INT) {
			int[] data = ((DataBufferInt) dataBuffer).getData();
			for (int i = 0, row = 0, col = 0; i < data.length; i++) {
				result[row][col] = data[i] & 0x00FFFFFF; // mask alpha
				col++;
				if (col == width) {
					col = 0;
					row++;
				}
			}
			return result;
		}

		final byte[] pixels = ((DataBufferByte) dataBuffer).getData();
		final boolean hasAlphaChannel = image.getAlphaRaster() != null;

		if (hasAlphaChannel) {
			final int pixelLength = 4;
			for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
				int argb = 0;
				argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
				argb += ((int) pixels[pixel + 1] & 0xff); // blue
				argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
				argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
				result[row][col] = argb;
				col++;
				if (col == width) {
					col = 0;
					row++;
				}
			}
		} else {
			final int pixelLength = 3;
			for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength) {
				int argb = 0;
				argb += -16777216; // 255 alpha
				argb += ((int) pixels[pixel] & 0xff); // blue
				argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
				argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
				result[row][col] = argb;
				col++;
				if (col == width) {
					col = 0;
					row++;
				}
			}
		}

		return result;
	}

	@Override
	public Frame convert(String filename, DMD dmd, int frameNo) {
		return super.convert(filename, dmd, frameNo-skip);
	}

}
