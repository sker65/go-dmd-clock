package com.rinke.solutions.pinball.ui;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.io.GifSequenceWriter;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.renderer.ImageUtil;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.widget.DMDWidget;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

@Bean
@Slf4j
public class GifExporter extends Dialog {
    
    protected Object result;
    protected Shell shell;
    private String lastPath;
    @Autowired FileChooserUtil fileChooserUtil;

	private ProgressBar progressBar;
	
	private GifSequenceWriter gifWriter;
	@Setter Animation ani;
	@Setter Palette palette;
	Display display;

	private String filename;

	private Thread writer;

	private volatile boolean abort = false;

	private Combo comboSize;
	private Label lblPitch;
	private Label lblMargin;
	private Text marginTxt;
	private Label lblMaxFrame;
	private Text maxFrameTxt;
	private int margin;
	private int maxFrame;
	private int pitch = 3;

    /**
     * Create the dialog.
     * @param parent
     * @param style
     */
    public GifExporter() {
        super(new Shell(), SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
    }
    
    protected DMDWidget getDmdWidget(Composite parent, int style, DMD dmd, boolean scrollable) {
    	return new DMDWidget(shell, style, dmd, scrollable);
    }

    public void exportAni(String filename) {
    	exportAni( filename, false, Integer.MAX_VALUE);
    }
    
    public void exportAni(String filename, final boolean yield, int maxFrame) {
		DMD dmd = new DMD(ani.width, ani.height);
		
		DMDWidget widget = getDmdWidget(shell, 0, dmd, false);
		widget.setPalette(palette);
		int pitch = 1;
		if (comboSize.getSelectionIndex() > 0)
			pitch = comboSize.getSelectionIndex() + 2;
		widget.setMargin(margin);
		int width = (dmd.getWidth()+0) * pitch + widget.getMargin();
		int height = (dmd.getHeight()+0) * pitch + widget.getMargin();
		widget.setBounds(0, 0, width, height);
		// override margin
		widget.setMargin(margin);
		widget.setVisible(false);
		widget.setPitch(pitch);
		int saveActframe = ani.actFrame;
		ani.actFrame = 0;
		progressBar.setMinimum(0);
		int maximum = maxFrame<ani.getFrameCount(dmd)?maxFrame:ani.getFrameCount(dmd);
		progressBar.setMaximum(maximum);
	
		if( ani instanceof CompiledAnimation ) {
			CompiledAnimation cani = (CompiledAnimation)ani;
			dmd.setNumberOfPlanes(cani.frames.get(0).planes.size());
		}
		
		ImageOutputStream outputStream;
		try {
			outputStream = new FileImageOutputStream(new File(filename));
			gifWriter = new GifSequenceWriter(outputStream,
					BufferedImage.TYPE_INT_ARGB, 1000, false);

			Runnable r = ()->{
				try {
					while (true) {
						if( yield ) Thread.yield();
						dmd.clear();
						Frame res = ani.render(dmd, false);
						dmd.writeOr(res);
						Image swtImage = widget.drawImage(display, width, height);
						
						gifWriter.writeToSequence(ImageUtil.convert(swtImage), ani.getRefreshDelay());
						
						display.asyncExec(()->{
							if( ani.actFrame>0 && !progressBar.isDisposed()) progressBar.setSelection(ani.actFrame);	
						});
	
						log.info("exporting frame {} to {}", ani.actFrame, filename);
						if (ani.actFrame >= maxFrame-1 || abort || ani.hasEnded()) {
							if (ani.actFrame > 0) {
								dmd.clear();
								Frame last = ani.render(dmd, false);
								dmd.writeOr(last);
								Image lastSwtImage = widget.drawImage(display, width, height);
								gifWriter.writeToSequence(ImageUtil.convert(lastSwtImage), ani.getRefreshDelay());
							}
							break;
						}
							
					}
				} catch( IOException /*| InterruptedException*/ e) {
					log.error("error exporting to {}", filename);
					throw new RuntimeException("error eporting to " + filename, e);
				} finally {
					ani.actFrame = saveActframe;
					try {
						gifWriter.close();
						outputStream.close();
					} catch (IOException e) {
					}
				}
			};
			if( yield ) {
				writer = new Thread(r);
				writer.start();
			} else { // for testing
				r.run();
			}

		} catch (IOException e) {
			log.error("error exporting to {}", filename);
			throw new RuntimeException("error eporting to " + filename, e);
		}

	}
    
    protected void save() {
    	filename = fileChooserUtil.choose(SWT.SAVE, lastPath, new String[] { "*.gif" }, new String[] {  "animated gif" });
        if (filename != null) {
        	margin = saveParseInt(marginTxt.getText());
        	maxFrame = saveParseInt(maxFrameTxt.getText());
            exportAni(filename, true, maxFrame==0 ? Integer.MAX_VALUE:maxFrame);
        }
    }

    private int saveParseInt(String text) {
		try {
			return Integer.parseInt(text);
		} catch ( NumberFormatException e) {
			return 0;
		}
	}

	/**
     * Open the dialog.
     * @return the result
     */
    public Object open() {
    	createContents();
        shell.open();
        shell.layout();
        marginTxt.setText(String.valueOf(margin));
        maxFrameTxt.setText(String.valueOf(maxFrame));
        display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
            if( writer != null && !writer.isAlive() ) shell.close();
        }
        // delete Thread
        writer = null;
        return result;
    }

    /**
 	 * @wbp.parser.entryPoint
     * Create contents of the dialog.
     */
    void createContents() {
        shell = new Shell(getParent(), getStyle());
        shell.setSize(448, 203);
        shell.setText("Export to animated Gif");
        shell.setLayout(new FormLayout());
        
        Group grpConfig = new Group(shell, SWT.NONE);
        FormData fd_grpConfig = new FormData();
        fd_grpConfig.top = new FormAttachment(0, 10);
        fd_grpConfig.left = new FormAttachment(0, 10);
        fd_grpConfig.bottom = new FormAttachment(0, 193);
        fd_grpConfig.right = new FormAttachment(0, 440);
        grpConfig.setLayoutData(fd_grpConfig);
        grpConfig.setText("Export Settings");
        grpConfig.setLayout(new GridLayout(3, false));
        
        lblPitch = new Label(grpConfig, SWT.NONE);
        lblPitch.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblPitch.setText("Dot-Size");
        
        comboSize = new Combo(grpConfig, SWT.NONE);
        GridData gd_comboSize = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gd_comboSize.widthHint = 99;
        comboSize.setLayoutData(gd_comboSize);
        comboSize.setItems(new String[] {"1","2","3","4","6","8","10"});
        comboSize.select(0);
        
        Button btnSave = new Button(grpConfig, SWT.NONE);
        btnSave.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnSave.setText("Save");
        btnSave.addListener(SWT.Selection, e->save());
        
        lblMargin = new Label(grpConfig, SWT.NONE);
        lblMargin.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblMargin.setText("Margin:");
        
        marginTxt = new Text(grpConfig, SWT.BORDER);
        marginTxt.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        
        Button btnCancel = new Button(grpConfig, SWT.NONE);
        btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnCancel.setText("Cancel");
        btnCancel.addListener(SWT.Selection, e->abort());
        
        lblMaxFrame = new Label(grpConfig, SWT.NONE);
        lblMaxFrame.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblMaxFrame.setText("Max Frame:");
        
        maxFrameTxt = new Text(grpConfig, SWT.BORDER);
        maxFrameTxt.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        new Label(grpConfig, SWT.NONE);
        
        progressBar = new ProgressBar(grpConfig, SWT.NONE);
        GridData gd_progressBar = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
        gd_progressBar.widthHint = 111;
        progressBar.setLayoutData(gd_progressBar);
        new Label(grpConfig, SWT.NONE);
    }


	private void abort() {
		abort = true;
		shell.close();
	}

}
