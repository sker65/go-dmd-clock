package com.rinke.solutions.pinball;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


public class GoDMDClock {

	private DMD dmd = new DMD();
    
	private AnimationHandler animationHandler;

    private Shell shell;

    private DMDClock clock = new DMDClock(false);
    
    public GoDMDClock(Display display) {

        shell = new Shell(display);

        shell.setLayout(new FillLayout());
        shell.setText("DMD");
        shell.setSize(960, 320);
        shell.setLocation(300, 300);

        Canvas canvas = new Canvas(shell, SWT.NONE);
        canvas.addPaintListener(new ColorsPaintListener());

        canvas.setBackground(new Color(display,10,10,10));

        List<Animation> anis = null;
		try {
			anis = 
					AnimationFactory.buildAnimations();
			//AnimationCompiler.readFromCompiledFile("foo.ani");
			//AnimationCompiler.readFromRunDMDFile("/home/sr/Downloads/Pinball/RunDMD_B106_AO.imgc");
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        animationHandler = new AnimationHandler(anis,clock,dmd,canvas);       
        animationHandler.setShell(shell);
        
        shell.open();

        display.timerExec(animationHandler.getRefreshDelay(),  animationHandler);
        
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private class ColorsPaintListener implements PaintListener {

		public void paintControl(PaintEvent e) {
        	dmd.draw(e);
        	if( animationHandler != null ) e.display.timerExec(animationHandler.getRefreshDelay(), animationHandler);
            e.gc.dispose();
        }
    }
    
    public static void main(String[] args) {
        Display display = new Display();
        new GoDMDClock(display);
        display.dispose();
    }
}
