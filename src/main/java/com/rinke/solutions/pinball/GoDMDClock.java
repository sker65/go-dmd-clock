package com.rinke.solutions.pinball;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class GoDMDClock {

	private DMD dmd = new DMD();
    
	private AnimationHandler animationHandler;

    private Shell shell;

    private DMDClock clock = new DMDClock(false);
    
    List<Animation> anis = null;
    
    String filename;
    
    public GoDMDClock(Display display, String filename) {
    	
    	this.filename = filename;
    	int cols = 10;
        shell = new Shell(display);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = cols;
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
        gridData.horizontalSpan = cols;
        gridData.verticalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        canvas.setLayoutData(gridData);

        
		try {
			anis = 
			   AnimationFactory.buildAnimations(filename);
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
        Button prev = new Button(shell, SWT.PUSH);
        prev.setText("-");
        prev.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				animationHandler.prev();
				
			}
		});
        Button next = new Button(shell, SWT.PUSH);
        next.setText("+");
        next.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				animationHandler.next();
				
			}
		});
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.horizontalSpan = 2;
        final Scale scale = new Scale(shell, SWT.HORIZONTAL);
        scale.setLayoutData(gd);
        scale.setMinimum(0);
        scale.setMaximum(100);
        scale.setIncrement(1);
        animationHandler.setScale(scale);
        scale.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				animationHandler.setPos(scale.getSelection());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
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
        String filename = "animations4.properties";
        if( args.length>0 ) filename = args[0];
        new GoDMDClock(display, filename);
        display.dispose();
    }
}
