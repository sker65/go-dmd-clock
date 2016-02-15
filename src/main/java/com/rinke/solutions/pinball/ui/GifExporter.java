package com.rinke.solutions.pinball.ui;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.io.GifSequenceWriter;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.widget.DMDWidget;

public class GifExporter extends Dialog {
    
    private static final Logger LOG = LoggerFactory.getLogger(GifExporter.class);

    protected Object result;
    protected Shell shell;
    private String lastPath;

	private ProgressBar progressBar;
	
	private GifSequenceWriter gifWriter;
	Animation ani;
	Palette palette;
	Display display;

	private String filename;

	private Thread writer;

	private volatile boolean abort = false;

	private Combo comboSize;

    /**
     * Create the dialog.
     * @param parent
     * @param style
     */
    public GifExporter(Shell parent, Palette palette, Animation ani) {
        super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
        //setText("Device Config");
        this.ani = ani;
        this.palette = palette;
    }
    

    public void exportAni(String filename) {
		DMD dmd = new DMD(128, 32);
		
		DMDWidget dmdWidget = new DMDWidget(shell, 0, dmd);
		dmdWidget.setPalette(palette);
		int pitch = comboSize.getSelectionIndex() + 2;
		int width = dmd.getWidth() * pitch +20;
		int height = dmd.getHeight() * pitch +20;;
		dmdWidget.setBounds(0, 0, width, height);
		
		progressBar.setMinimum(0);
		progressBar.setMaximum(ani.getFrameCount(dmd));
		
		ImageOutputStream outputStream;
		try {
			outputStream = new FileImageOutputStream(new File(filename));
			gifWriter = new GifSequenceWriter(outputStream,
					BufferedImage.TYPE_INT_ARGB, 1000, false);

			writer = new Thread(()->{
				try {
					while (true) {
						Thread.yield();
						dmd.clear();
						Frame res = ani.render(dmd, false);
						dmd.writeOr(res);
						Image swtImage = dmdWidget.drawImage(display, width, height);
						
						gifWriter.writeToSequence(convert(swtImage), ani.getRefreshDelay());
						
						display.asyncExec(()->progressBar.setSelection(ani.actFrame));
	
						LOG.info("exporting frame {} to {}", ani.actFrame, filename);
						if (abort || ani.hasEnded())
							break;
					}
				} catch( IOException /*| InterruptedException*/ e) {
					LOG.error("error exporting to {}", filename);
					throw new RuntimeException("error eporting to " + filename, e);
				} finally {
					try {
						gifWriter.close();
					} catch (IOException e) {
					}
				}
			});
			
			writer.start();

		} catch (IOException e) {
			LOG.error("error exporting to {}", filename);
			throw new RuntimeException("error eporting to " + filename, e);
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

    // testability overridden by tests
    protected FileChooser createFileChooser(Shell shell, int flags) {   
        return new FileDialogDelegate(shell, flags);
    }
    
    protected void save() {
        FileChooser fileChooser = createFileChooser(shell, SWT.SAVE);
        fileChooser.setOverwrite(true);
        //fileChooser.setFileName("pin2dmd.dat");
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.gif" });
        fileChooser.setFilterNames(new String[] {  "animated gif" });
        filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();        
        if (filename != null) {
            exportAni(filename);
        }
        //shell.close();
    }


    /**
     * Open the dialog.
     * @return the result
     */
    public Object open() {
        createContents();
        shell.open();
        shell.layout();
        display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
            if( writer != null && !writer.isAlive() ) shell.close();
        }
        return result;
    }

    /**
     * Create contents of the dialog.
     */
    void createContents() {
        shell = new Shell(getParent(), getStyle());
        shell.setSize(448, 162);
        shell.setText("Export to animated Gif");
        shell.setLayout(new FormLayout());
        
        Group grpConfig = new Group(shell, SWT.NONE);
        FormData fd_grpConfig = new FormData();
        fd_grpConfig.top = new FormAttachment(0, 10);
        fd_grpConfig.left = new FormAttachment(0, 10);
        fd_grpConfig.bottom = new FormAttachment(0, 129);
        fd_grpConfig.right = new FormAttachment(0, 440);
        grpConfig.setLayoutData(fd_grpConfig);
        grpConfig.setText("Export Settings");
        grpConfig.setLayout(new GridLayout(2, false));
        
        comboSize = new Combo(grpConfig, SWT.NONE);
        GridData gd_comboSize = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gd_comboSize.widthHint = 99;
        comboSize.setLayoutData(gd_comboSize);
        comboSize.setItems(new String[] {"2","3","4"});
        comboSize.select(0);
        
        Button btnSave = new Button(grpConfig, SWT.NONE);
        btnSave.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnSave.setText("Save");
        btnSave.addListener(SWT.Selection, e->save());
        
        progressBar = new ProgressBar(grpConfig, SWT.NONE);
        progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        
        Button btnCancel = new Button(grpConfig, SWT.NONE);
        btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnCancel.setText("Cancel");
        btnCancel.addListener(SWT.Selection, e->abort());
    }


	private void abort() {
		abort = true;
		shell.close();
	}
}
