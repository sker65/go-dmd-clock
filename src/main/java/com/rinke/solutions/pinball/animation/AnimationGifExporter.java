package com.rinke.solutions.pinball.animation;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.io.GifSequenceWriter;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.widget.DMDWidget;

public class AnimationGifExporter {

	private static Logger LOG = LoggerFactory
			.getLogger(AnimationGifExporter.class);

	private GifSequenceWriter gifWriter;

	public void export(String filename, Animation ani, Palette palette, Shell parent) {
		DMD dmd = new DMD(128, 32);
		
		DMDWidget dmdWidget = new DMDWidget(parent, 0, dmd);
		dmdWidget.setPalette(palette);
		int width = dmd.getWidth() * 3 +20;
		int height = dmd.getHeight() * 3 +20;;
		dmdWidget.setBounds(0, 0, width, height);
		
		ImageOutputStream outputStream;
		try {
			outputStream = new FileImageOutputStream(new File(filename));
			gifWriter = new GifSequenceWriter(outputStream,
					BufferedImage.TYPE_INT_ARGB, 1000, false);

			Display display = Display.getDefault();
			while (true) {
				dmd.clear();
				Frame res = ani.render(dmd, false);
				dmd.writeOr(res);
				Image swtImage = dmdWidget.drawImage(display, width, height);
				gifWriter.writeToSequence(convert(swtImage), ani.getRefreshDelay());
				LOG.info("exporting frame {} to {}", ani.actFrame, filename);
				if (ani.hasEnded())
					break;
			}

		} catch (IOException e) {
			LOG.error("error exporting to {}", filename);
			throw new RuntimeException("error eporting to " + filename, e);
		} finally {
			try {
				gifWriter.close();
			} catch (IOException e) {
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
	public BufferedImage convert(Image srcImage) {

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
				RGB color = imageData.palette.getRGB(imageData.getPixel(x, y));
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

}
