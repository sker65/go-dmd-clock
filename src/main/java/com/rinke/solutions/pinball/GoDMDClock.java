package com.rinke.solutions.pinball;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class GoDMDClock {

	private DMD dmd = new DMD();
    
	private AnimationHandler animationHandler;

    private Shell shell;

    private DMDClock clock = new DMDClock(false);
    
    List<Animation> anis = null;
    
    public GoDMDClock(Display display) {

        shell = new Shell(display);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 6;
        shell.setLayout(gridLayout);
        shell.setText("DMD");
        shell.setSize(960, 380);
        shell.setLocation(300, 300);

        Canvas canvas = new Canvas(shell, SWT.NONE);
        canvas.setSize(960, 320);
        canvas.addPaintListener(new ColorsPaintListener());
        canvas.setBackground(new Color(display,10,10,10));
        
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 6;
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        canvas.setLayoutData(gridData);

        
		try {
			anis = 
			   AnimationFactory.buildAnimations("animations.properties");
			// AnimationCompiler.readFromCompiledFile("foo.ani");
			// AnimationCompiler.readFromRunDMDFile("/home/sr/Downloads/Pinball/RunDMD_B106_AO.imgc");
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        animationHandler = new AnimationHandler(anis,clock,dmd,canvas);       
        animationHandler.setShell(shell);

        new Label(shell, SWT.SINGLE ).setText("Properties File: ");;
        Text propfile = new Text(shell, SWT.SINGLE | SWT.BORDER );
        new Button(shell, SWT.PUSH).setText("Load");
        Button compile = new Button(shell, SWT.PUSH);
        compile.setText("Compile");
        compile.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				compile();
			}
        	
        });
        Button start = new Button(shell, SWT.PUSH);
        start.setText("Start");
        start.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				animationHandler.start();
				
			}
		});
        Button stop = new Button(shell, SWT.PUSH);
        stop.setText("Stop");
        stop.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				animationHandler.stop();
				
			}
		});
        //shell.pack();
        shell.open();

        display.timerExec(animationHandler.getRefreshDelay(),  animationHandler);
        
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }
    
    private void compile() {
    	AnimationCompiler.compile(anis, "foo.ani");
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
