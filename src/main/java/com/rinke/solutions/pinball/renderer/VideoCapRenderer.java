package com.rinke.solutions.pinball.renderer;

import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.renderer.video.FFmpegFrameGrabber;
import com.rinke.solutions.pinball.renderer.video.Java2DFrameConverter;
import com.rinke.solutions.pinball.ui.Progress;

@Slf4j
public class VideoCapRenderer extends Renderer {

	private static Logger LOG = LoggerFactory.getLogger(VideoCapRenderer.class);
	private int skip = 0;
	String name;
	DMD dmd;
	DmdSize size = DmdSize.Size128x32;
	
	protected void readImage(String name, DMD dmd) {
		this.name = name;
		this.dmd = dmd;
		doReadImage();
	}

	@Override
	public void run() {
		doReadImage();
	}

	protected void doReadImage() {
		notify(0, "init grabber");
		
		size = getInt("width", 128) == 128 ? DmdSize.Size128x32 : DmdSize.Size192x64;
		
		Java2DFrameConverter converter = new Java2DFrameConverter();
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(name);
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
			grabber.start();
			for (int i = 0; i <end+1; i++) {
				Object frame;
				do {
					frame = grabber.grab();
					if( frame == null ) break;
				} while( !grabber.containsData(frame) );

				if( frame == null ) break;
				notify(i*100 / end, "importing "+i+"/"+end+" frames");
				if( i < skip-1 ) continue;
				
				BufferedImage image = converter.convert(frame);
				//int sh = image.getHeight();
				//int sw = image.getWidth();
				
				if( op != null ) image = op.filter(image, null);
				// extract clipping from props
				
				BufferedImage dmdImage = new BufferedImage(w,
						h, BufferedImage.TYPE_INT_RGB);

				Graphics graphics = dmdImage.createGraphics();
				graphics.drawImage(image.getSubimage(getInt("clipx",0), getInt("clipy",0), getInt("clipw",size.width), getInt("cliph",size.height)), 0, 0, w,
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
				Frame res = ImageUtil.convertToFrame(dmdImage, dmd.getWidth(), dmd.getHeight(),5);
				res.timecode = (int)( grabber.getTimestamp() / 1000 );
				frames.add(res);
				notify(50, "reading frame "+frames.size());
			} // stop cap
			grabber.stop();
			grabber.release();
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
		if (frames.isEmpty()) {
			readImage(filename, dmd);
		}
		return frames.get(frameNo-skip);
	}


}
