package com.rinke.solutions.pinball.renderer;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;

import org.eclipse.swt.graphics.Image;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.fappel.swt.DisplayHelper;


public class ImageUtilSWTTest {

	@Rule
	public final DisplayHelper displayHelper = new DisplayHelper();
	
	@Test
	public void testConvert() throws Exception {
		Image srcImage = new Image(displayHelper.getDisplay(), 200, 100);
		BufferedImage bufferedImage = ImageUtil.convert(srcImage );
		assertEquals(200, bufferedImage.getWidth());
	}


}
