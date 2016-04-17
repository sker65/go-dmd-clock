package com.rinke.solutions.pinball.widget.color;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Canvas;


public class ColorPicker {

	private static final int NRECENT = 6;  // how many recent color are preserved
	protected Shell shell;
	private Display display;
	private Text hexField;

	/** Used to indicate when we're in "hue mode". */
	public static final int HUE = 0;
	/** Used to indicate when we're in "brightness mode". */
	public static final int BRI = 1;
	/** Used to indicate when we're in "saturation mode". */
	public static final int SAT = 2;
	/** Used to indicate when we're in "red mode". */
	public static final int RED = 3;
	/** Used to indicate when we're in "green mode". */
	public static final int GREEN = 4;
	/** Used to indicate when we're in "blue mode". */
	public static final int BLUE = 5;
	
	private int mode = ColorPicker.HUE;

	private int currentRed = 0;
	private int currentGreen = 0;
	private int currentBlue = 0;
	private Spinner bri;
	private Spinner sat;
	private Spinner hue;
	private ColorPickerPanel colorPanel;
	private ColorPickerSlider cpSlider;
	/** Used to indicate when we're internally adjusting the value of the spinners.
	 * If this equals zero, then incoming events are triggered by the user and must be processed.
	 * If this is not equal to zero, then incoming events are triggered by another method
	 * that's already responding to the user's actions.
	 */
	private int adjustingSpinners = 0;

	/** Used to indicate when we're internally adjusting the value of the slider.
	 * If this equals zero, then incoming events are triggered by the user and must be processed.
	 * If this is not equal to zero, then incoming events are triggered by another method
	 * that's already responding to the user's actions.
	 */
	private int adjustingSlider = 0;

	/** Used to indicate when we're internally adjusting the selected color of the ColorPanel.
	 * If this equals zero, then incoming events are triggered by the user and must be processed.
	 * If this is not equal to zero, then incoming events are triggered by another method
	 * that's already responding to the user's actions.
	 */
	private int adjustingColorPanel = 0;

	/** Used to indicate when we're internally adjusting the value of the hex field.
	 * If this equals zero, then incoming events are triggered by the user and must be processed.
	 * If this is not equal to zero, then incoming events are triggered by another method
	 * that's already responding to the user's actions.
	 */
	private int adjustingHexField = 0;
	private Spinner red;
	private Spinner green;
	private Spinner blue;
	private ColorCanvas preview;
	private ColorCanvas[] recentCol = new ColorCanvas[NRECENT];
	private RGB[] recentRGB = new RGB[NRECENT];
	private RGB currentRGB;
	private boolean created;
	private boolean closed;
	private List<ColorModifiedListener> listeners = new ArrayList<>();
	private SetRGBRunnable rgbRunnable;

	public int getMode() {
		return mode;
	}
	
	/** @return the current HSB coordinates of this <code>ColorPicker</code>.
	 * Each value is between [0,1].
	 * 
	 */
	public float[] getHSB() {
		return new float[] {
				getFloatValue(hue)/360f,
				getFloatValue(sat)/100f,
				getFloatValue(bri)/100f
		};
	}

	private float getFloatValue(Spinner spinner) {
		return (float)spinner.getSelection();
	}

	/** @return the current RGB coordinates of this <code>ColorPicker</code>.
	 * Each value is between [0,255].
	 * 
	 */
	public int[] getRGB() {
		return new int[] {
				currentRed,
				currentGreen,
				currentBlue
		};
	}
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ColorPicker() {
		display = Display.getDefault();
		shell = new Shell(SWT.CLOSE | SWT.TITLE | SWT.MIN);
	}
	
	public void close() {
		closed = true;
		shell.setVisible(false);
	}
	
	public enum EventType { Selected, Choosing };
	
	public static class ColorModifiedEvent {
		public ColorModifiedEvent(EventType eventType, RGB rgb) {
			super();
			this.eventType = eventType;
			this.rgb = rgb;
		}
		public EventType eventType;
		public RGB rgb;
	}
	
	public interface ColorModifiedListener {
		public void colorModified( ColorModifiedEvent evt);
	}
	
	public void addListener( ColorModifiedListener listener ) {
		listeners.add(listener);
	}
	
	private void notifyListeners( EventType type, RGB rgb ) {
		ColorModifiedEvent event = new ColorModifiedEvent(type,rgb);
		listeners.forEach(e->e.colorModified(event));
	}
	
	public abstract class DelayedInfrequentAction implements Runnable {

	    private int delay;
	    private Display display;

	    private Timer timer = null;

	    public DelayedInfrequentAction(Display display, int delay) {
	        this.display = display;
	        this.delay = delay;
	    }
	    
	    public void cancel() {
	        if( timer != null ) {
	            timer.cancel();
	            timer.purge();
	            timer = null;
	        }
	    }

	    public synchronized void kick() {
	    	cancel();
	        timer = new Timer(this.getClass().getSimpleName(), true);
	        timer.schedule(new TimerTask() {

	            @Override
	            public void run() {
	                display.syncExec(DelayedInfrequentAction.this);
	                synchronized (DelayedInfrequentAction.this) {
	                    timer = null;
	                }

	            }}, delay);

	    }

	    @Override
	    abstract public void run();

	}
	
	class SetRGBRunnable extends DelayedInfrequentAction {
		final int red, green, blue;
		
		SetRGBRunnable(Display display, int delay, int red,int green,int blue) {
			super(display, delay);
			this.red = red;
			this.green = green;
			this.blue = blue;
		}
		
		public void run() {
			int pos = hexField.getCaretPosition();
			setRGB(red, green, blue);
			pos = Math.min(pos, hexField.getText().length());
			//hexField.setCaretPosition(pos);
		}
	}


	
	public void changedHexField() {
		if(adjustingHexField>0)
			return;
		
		String s = hexField.getText();
		s = stripToHex(s, 6);
		
		/* If we don't have 6 characters, then use a delay.
		 * If, after a second or two, the user has just
		 * stopped typing: then we can try to make
		 * sense of what they input even if its
		 * incomplete.
		 */
		boolean delay = false;
		if(s.length()<6) {
			delay = true;
			while(s.length()<6) {
				s = s+"0";
			}
		}
		
		try {
			int i = Integer.parseInt(s,16);
			int red = ((i >> 16) & 0xff);
			int green = ((i >> 8) & 0xff);
			int blue = ((i) & 0xff);
			
			if(delay) {
				cancelTimer();
				rgbRunnable = new SetRGBRunnable(display, 1000, red, green, blue);
				rgbRunnable.kick();
			} else {
				cancelTimer();
				setRGB(red, green, blue);
			}
			return;
		} catch(NumberFormatException e2) {
		}
		
	}
	
	private void cancelTimer() {
		if( rgbRunnable!=null) {
			rgbRunnable.cancel();
			rgbRunnable=null;
		}
	}

	/** Strips a string down to only uppercase hex-supported characters.
	 * @param s the string to strip
	 * @param charLimit the maximum number of characters in the return value
	 * @return an uppercase version of <code>s</code> that only includes hexadecimal
	 * characters and is not longer than <code>charLimit</code>. 
	 */
	private String stripToHex(String s,int charLimit) {
		s = s.toUpperCase();
		StringBuffer returnValue = new StringBuffer(6);
		for(int a = 0; a<s.length() && returnValue.length()<charLimit; a++) {
			char c = s.charAt(a);
			if(Character.isDigit(c) || (c>='A' && c<='F')) {
				returnValue.append(c);
			}
		}
		return returnValue.toString();
	}


	/**
	 * Open the dialog.
	 * @param rgb 
	 * @return the result
	 */
	public RGB open(RGB rgb) {
		closed = false;
		if( !created ) {
			created = true;
			createContents();
			shell.open();
			shell.layout();
		} else {
			shell.setVisible(true);
		}
		setRGB(rgb.red, rgb.green, rgb.blue);
		while (!shell.isDisposed() && !closed ) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		closed = false;
		return currentRGB;
	}
	
	public void sliderValueChanged(int v) {
		if(adjustingSlider>0)
			return;
		
		switch (mode) {
		case BRI:
			bri.setSelection(v);
			break;
		case HUE:
			hue.setSelection(v);
			break;
		case SAT:
			sat.setSelection(v);
			break;
		case RED:
			red.setSelection(v);
			break;
		case GREEN:
			green.setSelection(v);
			break;
		case BLUE:
			blue.setSelection(v);
			break;
		default:
			break;
		}

	}
	
	public void panelValueChanged(int v) {
		if(adjustingColorPanel>0)
			return;

		int mode = getMode();
		if(mode==HUE || mode==BRI || mode==SAT) {
			float[] hsb = colorPanel.getHSB();
			setHSB(hsb[0],hsb[1],hsb[2]);
		} else {
			int[] rgb = colorPanel.getRGB();
			setRGB(rgb[0],rgb[1],rgb[2]);
		}
	}
	
	/** Sets the current color of this <code>ColorPicker</code>
	 * 
	 * @param r the red value.  Must be between [0,255].
	 * @param g the green value.  Must be between [0,255].
	 * @param b the blue value.  Must be between [0,255].
	 */
	public void setRGB(int r,int g,int b) {
		if(r<0 || r>255)
			throw new IllegalArgumentException("The red value ("+r+") must be between [0,255].");
		if(g<0 || g>255)
			throw new IllegalArgumentException("The green value ("+g+") must be between [0,255].");
		if(b<0 || b>255)
			throw new IllegalArgumentException("The blue value ("+b+") must be between [0,255].");
		
		Color lastColor = getColor();
		
		boolean updateRGBSpinners = adjustingSpinners==0;
		
		adjustingSpinners++;
		adjustingColorPanel++;
		//int alpha = this.alpha.getIntValue();
		try {
			if(updateRGBSpinners) {
				red.setSelection(r);
				green.setSelection(g);
				blue.setSelection(b);
			}
			float[] hsb = new float[3];
			Color.RGBtoHSB(r, g, b, hsb);
			hue.setSelection( (int)(hsb[0]*360f+.49f));
			sat.setSelection( (int)(hsb[1]*100f+.49f));
			bri.setSelection( (int)(hsb[2]*100f+.49f));
			colorPanel.setRGB(r, g, b);
			updateHexField();
			updateSlider();
			preview.setRGB(r, g, b);
		} finally {
			adjustingSpinners--;
			adjustingColorPanel--;
		}
		currentRed = r;
		currentGreen = g;
		currentBlue = b;
		notifyListeners(EventType.Choosing, new RGB(r,g,b));
	}
	

	
	
	private void updateSlider() {
		adjustingSlider++;
		try {
			int mode = getMode();
			if(mode==HUE) {
				cpSlider.setValue( getIntValue(hue) );
			} else if(mode==SAT) {
				cpSlider.setValue( getIntValue(sat) );
			} else if(mode==BRI) {
				cpSlider.setValue( getIntValue(bri) );
			} else if(mode==RED) {
				cpSlider.setValue( getIntValue(red) );
			} else if(mode==GREEN) {
				cpSlider.setValue( getIntValue(green) );
			} else if(mode==BLUE) {
				cpSlider.setValue( getIntValue(blue) );
			}
		} finally {
			adjustingSlider--;
		}
	}

	private int getIntValue(Spinner s) {
		return s.getSelection();
	}

	/** @return the current <code>Color</code> this <code>ColorPicker</code> has selected.
	 * <P>This is equivalent to:
	 * <BR><code>int[] i = getRGB();</code>
	 * <BR><code>return new Color(i[0], i[1], i[2], opacitySlider.getValue());</code>
	 */
	public Color getColor() {
		int[] i = getRGB();
		return new Color(i[0], i[1], i[2] );
	}

	/** Sets the current color of this <code>ColorPicker</code>
	 * 
	 * @param h the hue value.
	 * @param s the saturation value.  Must be between [0,1].
	 * @param b the blue value.  Must be between [0,1].
	 */
	public void setHSB(float h, float s, float b) {
		if(Float.isInfinite(h) || Float.isNaN(h))
			throw new IllegalArgumentException("The hue value ("+h+") is not a valid number.");
		//hue is cyclic, so it can be any value:
		while(h<0) h++;
		while(h>1) h--;
		
		if(s<0 || s>1)
			throw new IllegalArgumentException("The saturation value ("+s+") must be between [0,1]");
		if(b<0 || b>1)
			throw new IllegalArgumentException("The brightness value ("+b+") must be between [0,1]");
		
		Color lastColor = getColor();
		
		boolean updateHSBSpinners = adjustingSpinners==0;
		adjustingSpinners++;
		adjustingColorPanel++;
		try {
			if(updateHSBSpinners) {
				hue.setSelection( (int)(h*360f+.49f));
				sat.setSelection( (int)(s*100f+.49f));
				bri.setSelection( (int)(b*100f+.49f));
			}
			
			Color c = new Color(Color.HSBtoRGB(h, s, b));
			//int alpha = this.alpha.getIntValue();
			c = new Color(c.getRed(), c.getGreen(), c.getBlue());
			// preview.setForeground(c);
			currentRed = c.getRed();
			currentGreen = c.getGreen();
			currentBlue = c.getBlue();
			red.setSelection(currentRed);
			green.setSelection(currentGreen);
			blue.setSelection(currentBlue);
			colorPanel.setHSB(h, s, b);
			preview.setRGB(currentRed, currentGreen, currentBlue);
			updateHexField();
			updateSlider();
		} finally {
			adjustingSpinners--;
			adjustingColorPanel--;
		}
		Color newColor = getColor();
		notifyListeners(EventType.Choosing, new RGB(currentRed, currentGreen, currentBlue));
//		if(lastColor.equals(newColor)==false)
//			firePropertyChange(SELECTED_COLOR_PROPERTY,lastColor,newColor);
	}

	private void updateHexField() {
		adjustingHexField++;
		try {
			int r = red.getSelection();
			int g = green.getSelection();
			int b = blue.getSelection();
			
			int i = (r << 16) + (g << 8) +b;
			String s = Integer.toHexString(i).toUpperCase();
			while(s.length()<6)
				s = "0"+s;
			if(hexField.getText().equalsIgnoreCase(s)==false)
				hexField.setText(s);
		} finally {
			adjustingHexField--;
		}
	}


	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
	    
		shell.setSize(490, 444);
		shell.setText("Color Picker");
		shell.setLayout(new GridLayout(3, false));

		ResourceManager resManager = 
                new LocalResourceManager(JFaceResources.getResources(),shell);
		
		Label lblTest = new Label(shell, SWT.NONE);
		lblTest.setBounds(10, 10, 59, 14);
		lblTest.setText("Choose Color");
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		composite.setBounds(20, 30, 481, 347);
		composite.setLayout(new GridLayout(5, false));
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		
		colorPanel = new ColorPickerPanel(composite, this, SWT.NONE);
		GridData gd_colorPanel = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 8);
		gd_colorPanel.heightHint = 217;
		gd_colorPanel.widthHint = 220;
		colorPanel.setLayoutData(gd_colorPanel);
				
		cpSlider = new ColorPickerSlider(composite, this, 0);
		GridData gd_cpSlider = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 8);
		gd_cpSlider.heightHint = 214;
		gd_cpSlider.widthHint = 32;
		cpSlider.setLayoutData(gd_cpSlider);
		
		new Label(composite, SWT.NONE);
		
		preview = new ColorCanvas(composite, SWT.NONE, null);
		GridData gd_canvas = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_canvas.widthHint = 31;
		gd_canvas.heightHint = 31;
		preview.setLayoutData(gd_canvas);
		new Label(composite, SWT.NONE);
		
		Label lblHue = new Label(composite, SWT.NONE);
		lblHue.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblHue.setText("Hue:");
		
		hue = createSpinner(composite, 360);
		
		Button btnHue = new Button(composite, SWT.RADIO);
		btnHue.addListener(SWT.Selection, e->setMode(ColorPicker.HUE));
		btnHue.setSelection(true);
		
		Label lblSat = new Label(composite, SWT.NONE);
		lblSat.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblSat.setText("Sat:");
		
		sat = createSpinner(composite, 100);
		
		Button btnSat = new Button(composite, SWT.RADIO);
		btnSat.addListener(SWT.Selection, e->setMode(ColorPicker.SAT));
		
		Label lblBri = new Label(composite, SWT.NONE);
		lblBri.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblBri.setText("Bri:");
		
		bri = createSpinner(composite, 100);
		
		Button btnBri = new Button(composite, SWT.RADIO);
		btnBri.addListener(SWT.Selection, e->setMode(ColorPicker.BRI));

		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		
		Label lblRed = new Label(composite, SWT.NONE);
		lblRed.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblRed.setText("Red:");
		
		red = createSpinner(composite, 255);
	
		Button btnRed = new Button(composite, SWT.RADIO);
		btnRed.addListener(SWT.Selection, e->setMode(ColorPicker.RED));
		
		Label lblGreen = new Label(composite, SWT.NONE);
		lblGreen.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblGreen.setText("Green:");
		
		green = createSpinner(composite, 255);
		
		Button btnGreen = new Button(composite, SWT.RADIO);
		btnGreen.addListener(SWT.Selection, e->setMode(ColorPicker.GREEN));
		
		Label lblBlue = new Label(composite, SWT.NONE);
		lblBlue.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
		lblBlue.setText("Blue:");
		
		blue = createSpinner(composite, 255);
		
		Button btnBlue = new Button(composite, SWT.RADIO);
		btnBlue.addListener(SWT.Selection, e->setMode(ColorPicker.BLUE));
		
		Composite composite_1 = new Composite(composite, SWT.NONE);
		composite_1.setLayout(new GridLayout(NRECENT, false));
		GridData gd_composite_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite_1.heightHint = 43;
		gd_composite_1.widthHint = 222;
		composite_1.setLayoutData(gd_composite_1);

		for( int i = 0; i<NRECENT; i++) {
			recentCol[i] = new ColorCanvas(composite_1, SWT.NONE, this);
			GridData gd_colorCanvas_1 = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
			gd_colorCanvas_1.widthHint = 31;
			gd_colorCanvas_1.heightHint = 31;
			recentCol[i].setLayoutData(gd_colorCanvas_1);
		}
		
		new Label(composite, SWT.NONE);
		
		Label lblHex = new Label(composite, SWT.NONE);
		lblHex.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
		lblHex.setText("Hex:");
		
		hexField = new Text(composite, SWT.BORDER);
		GridData gd_text = new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1);
		gd_text.widthHint = 61;
		hexField.setLayoutData(gd_text);
		hexField.addModifyListener(e->changedHexField());
		new Label(composite, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		//new Label(shell, SWT.NONE);
		
		Label label = new Label(shell, SWT.NONE);
		GridData gd_label = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_label.widthHint = 276;
		label.setLayoutData(gd_label);
		label.setText("   ");
		
		Button btnCancelButton = new Button(shell, SWT.NONE);
		btnCancelButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		btnCancelButton.setText("Cancel");
		btnCancelButton.addListener(SWT.Selection, e->onCancel());
		
		Button btnApplyButton = new Button(shell, SWT.NONE);
		btnApplyButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		btnApplyButton.setText("Apply");
		btnApplyButton.addListener(SWT.Selection, e->onApply());

		Button btnOk = new Button(shell, SWT.NONE);
		GridData gd_btnOk = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		gd_btnOk.widthHint = 74;
		btnOk.setLayoutData(gd_btnOk);
		btnOk.addListener(SWT.Selection, e->onOk());
		btnOk.setBounds(545, 398, 94, 28);
		btnOk.setText("OK");
	}

	private void onApply() {
		currentRGB = new RGB(currentRed,currentGreen,currentBlue);
		notifyListeners(EventType.Selected, currentRGB);
		rotateRecent();
	}

	private void onCancel() {
		currentRGB = null;
		close();
	}

	private void onOk() {
		currentRGB = new RGB(currentRed,currentGreen,currentBlue);
		notifyListeners(EventType.Selected, currentRGB);
		rotateRecent();
		close();
	}

	private void rotateRecent() {
		for(int i = 0; i < NRECENT; i++) {
			if( recentRGB[i] !=null && recentRGB[i].equals(currentRGB)) return;
		}
		for(int i = NRECENT-1; i > 0; i--) {
			recentRGB[i] = recentRGB[i-1];
		}
		recentRGB[0] = currentRGB;
		for(int i = 0; i < NRECENT; i++) {
			recentCol[i].setRgb(recentRGB[i]);
		}
	}

	private Spinner createSpinner(Composite composite, int max) {
		Spinner sp = new Spinner(composite, SWT.BORDER);
		sp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
		sp.addModifyListener(e->updateSpinner(e));
		sp.setMinimum(0);
		sp.setMaximum(max);
		sp.setIncrement(5);
		return sp;
	}
	
	private void updateSpinner(ModifyEvent e) {
		Spinner src = (Spinner) e.getSource();
		if(src.equals(hue) || src.equals(bri) || src.equals(sat) ) {
			if(adjustingSpinners>0)
				return;
			
			setHSB( hue.getSelection()/360f,
					sat.getSelection()/100f,
					bri.getSelection()/100f );
		} else {
			if(adjustingSpinners>0)
				return;
			
			setRGB( red.getSelection(),
					green.getSelection(),
					blue.getSelection() );
			
		}
	}

	private void setMode(int m) {
		mode = m;
		cpSlider.redraw();
		colorPanel.setMode(m);;
	}

	public static void main( String[] args ) {
		new ColorPicker().open(new RGB(255, 0, 0));
	}

	public ColorPickerPanel getColorPanel() {
		return colorPanel;
	}
}
