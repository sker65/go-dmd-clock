package com.rinke.solutions.pinball.animation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.eclipse.swt.graphics.Image;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.io.GifSequenceWriter;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.renderer.ImageUtil;
import com.rinke.solutions.pinball.widget.DMDWidget;

public class AnimationGifExporter {

	private static Logger LOG = LoggerFactory
			.getLogger(AnimationGifExporter.class);

	private GifSequenceWriter gifWriter;

	public void export(String filename, Animation ani, Palette palette, Shell parent) {
		DMD dmd = new DMD(PinDmdEditor.DMD_WIDTH,PinDmdEditor.DMD_HEIGHT);
		
		DMDWidget dmdWidget = new DMDWidget(parent, 0, dmd, false);
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
				gifWriter.writeToSequence(ImageUtil.convert(swtImage), ani.getRefreshDelay());
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

}
