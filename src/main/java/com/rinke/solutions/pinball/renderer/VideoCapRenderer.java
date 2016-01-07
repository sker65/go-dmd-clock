package com.rinke.solutions.pinball.renderer;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.animation.Frame;
import com.rinke.solutions.pinball.animation.Plane;

public class VideoCapRenderer extends Renderer {

	private static Logger LOG = LoggerFactory.getLogger(VideoCapRenderer.class);

	public VideoCapRenderer(int start, int end) {
		// TODO Auto-generated constructor stub
	}

	protected void readImage(String name, DMD dmd) {

		Java2DFrameConverter converter = new Java2DFrameConverter();
		FFmpegFrameGrabber g = new FFmpegFrameGrabber(name);
		int w = dmd.getWidth();
		int h = dmd.getHeight();
		int frameNo = 0;
		try {
			g.start();
			for (int i = 0; i < 700; i++) {
				org.bytedeco.javacv.Frame frame;
				do {
					frame = g.grab();
					if( frame == null ) break;
				} while( frame.image == null );

				if( frame == null ) break;
				
				BufferedImage image = converter.convert(frame);
				int sh = image.getHeight();
				int sw = image.getWidth();
				
				
				BufferedImage dmdImage = new BufferedImage(w,
						h, BufferedImage.TYPE_INT_RGB);

				Graphics graphics = dmdImage.createGraphics();
				graphics.drawImage(image.getSubimage(0, 35, sw, sh-40), 0, 0, w,
						h, null);
				graphics.dispose();
				
//				try {
//					ImageIO.write(dmdImage, "PNG",
//							new File(String.format("/Users/stefanri/tmp/%d-%b-cap%d.png", bufLen, kf, i)));
//				} catch (IOException e) {
//					e.printStackTrace();
//				}

				/*
				 * AffineTransform tx = new AffineTransform(); tx.scale(1, 2);
				 * AffineTransformOp op = new AffineTransformOp(tx,
				 * AffineTransformOp.TYPE_BILINEAR); bufferedImage =
				 * op.filter(bufferedImage, null);
				 */

//				int[][] image2d = convertTo2D(dmdImage);
				// int[] palette = Quantize.quantizeImage(image2d, 255);

				// create 
				Frame res = new Frame(w,h);
				for( int j = 0; j < 12 ; j++) {
					res.planes.add(new Plane((byte)j, new byte[dmd.getFrameSizeInByte()]));
				}
				res.timecode = (int)( g.getTimestamp() / 1000 );
				
				for (int x = 0; x < dmd.getWidth(); x++) {
					for (int y = 0; y < dmd.getHeight(); y++) {

						int rgb = dmdImage.getRGB(x, y);
						
						// reduce color depth to 12 bit
						int nrgb = ( rgb >> 4 ) & 0x0F;
						nrgb |= ( ( rgb >> 12 ) & 0x0F ) << 4;
						nrgb |= ( ( rgb >> 20 ) & 0x0F ) << 8;
						
						for( int j = 0; j < 12 ; j++) {
							if( (nrgb & (1<<j)) != 0)
								res.planes.get(j).plane[y * dmd.getBytesPerRow() + x / 8] |= (128 >> (x % 8));
						}

					}
				}
				frames.add(res);
				frameNo++;
			}
			g.stop();
		} catch (Exception e) {
			LOG.error("problems grabbing {}", name, e);
		}
		this.maxFrame = frameNo;
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

}
