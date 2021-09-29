package com.rinke.solutions.pinball.ui;

import static com.rinke.solutions.pinball.widget.SWTUtil.toSwtRGB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Scope;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.view.View;

//@Slf4j
@Bean(name="palettePicker", scope=Scope.PROTOTYPE)
public class PalettePicker extends Dialog implements View {

	protected Collection<RGB> result;
	protected Shell shell;
	Display display;
	ResourceManager resManager;

	List<RGB> colors = new ArrayList<>();
	private boolean open;
	private ColorListProvider colorListProvider;
	private int maxColors = 16;
	
	@FunctionalInterface
	public interface ColorListProvider {
		List<RGB> refreshColors(int accuracy);
	}
	
	public void setColorListProvider(ColorListProvider p) {
		this.colorListProvider = p;
	}

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public PalettePicker(Shell parent) {
		super(parent, SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
		this.display = parent.getDisplay();
		setText("Palette Picker");
		resManager = new LocalResourceManager(JFaceResources.getResources(),
				parent);
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public void open() {
		// for the sake of window builder
		//create();
		if( toolBar == null || shell.isDisposed() ) create();
		else updateColorButtons();
		open = true;
		shell.setVisible(true);
		
		while (open && !shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	private void selectAll() {
		deselectAll();
		for (int j = 0; j < colBtn.length; j++) {
			ToolItem i = colBtn[j];
			if( selectedColors.size() < maxColors ) {
				if( i.getImage() != null ) {
					RGB c = (RGB)i.getData();
					selectedColors.add(c);
					numOfSelectedColors++;
					i.setSelection(true);
				}
			}
		}
		updateLabels();
	}

	private void deselectAll() {
		for( ToolItem i : colBtn) {
			i.setSelection(false);
		}
		selectedColors.clear();
		numOfSelectedColors = 0;
		updateLabels();
	}

	public void create() {
		createContents();
		shell.open();
		shell.layout();
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(475, 490);
		shell.setText("Palette Picker");
		
		Button btnUpdatePalette = new Button(shell, SWT.NONE);
		btnUpdatePalette.setBounds(312, 415, 128, 28);
		btnUpdatePalette.setText("Update Palette");
		btnUpdatePalette.addListener(SWT.Selection, e->updatePalette());
		
		Button btnCancel = new Button(shell, SWT.NONE);
		btnCancel.setBounds(224, 415, 86, 28);
		btnCancel.setText("Cancel");
		btnCancel.addListener(SWT.Selection, e->cancel());
		
		Group grpColorsInScene = new Group(shell, SWT.NONE);
		grpColorsInScene.setText("Colors in Scene");
		grpColorsInScene.setBounds(10, 10, 430, 377);
		
		toolBar = new ToolBar(grpColorsInScene, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
		toolBar.setBounds(15, 15, 410, 360);
		
		lblSelectedColors = new Label(shell, SWT.NONE);
		lblSelectedColors.setBounds(194, 395, 117, 14);
		lblSelectedColors.setText("Selected Colors: 0");

		lblTotal = new Label(shell, SWT.NONE);
		lblTotal.setBounds(317, 395, 103, 14);
		lblTotal.setText("Total: 0");
		
		// create random palette
		int numberOfItems = 22*17;
		colBtn = new ToolItem[numberOfItems];
		updateColorButtons();
		
		Scale scale = new Scale(shell, SWT.NONE);
		scale.setBounds(24, 394, 150, 15);
		scale.setMinimum(0);
		scale.setIncrement(5);
		scale.setMaximum(100);
		scale.addListener(SWT.Selection, e->{ accuracy = scale.getSelection(); updateColorButtons(); });
		
		Label lblColorAccuracy = new Label(shell, SWT.NONE);
		lblColorAccuracy.setBounds(24, 415, 90, 20);
		lblColorAccuracy.setText("Color Accuracy");

		Button btnAll = new Button(shell, SWT.NONE);
		btnAll.setBounds(175, 415, 53, 28);
		btnAll.setText("All");
		btnAll.addListener(SWT.Selection, e->selectAll());
		
		Button btnNone = new Button(shell, SWT.NONE);
		btnNone.setBounds(116, 415, 60, 28);
		btnNone.setText("None");
		btnNone.addListener(SWT.Selection, e->deselectAll());
		

	}
	
	private void cancel() {
		result = null;
		close();
	}

	private void updatePalette() {
		boolean colorFound = false;
		selectedColors.clear();
		numOfSelectedColors=0;
		for (int j = 0; j < colBtn.length; j++) {
			ToolItem i = colBtn[j];
			if( i.getImage() != null ) {
				if (i.getSelection()) {
					RGB c = (RGB)i.getData();
					selectedColors.add(c);
					numOfSelectedColors++;
					colorFound = true;
				}
			} else {
				if(!colorFound) {
					selectedColors.add(null);
				}
				colorFound = false;
			}
		}
		result = selectedColors;
		close();
	}

	private void close() {
		this.open = false;
		shell.setVisible(false);
	}

	ToolItem colBtn[] = null;
	private int accuracy;
	
	private void updateColorButtons() {
		if( colorListProvider != null ) {
			colors = colorListProvider.refreshColors(accuracy);
			for (Iterator<RGB> i = selectedColors.iterator(); i.hasNext();) {
				RGB rgb = i.next();
				if( !colors.contains(rgb) ) i.remove();
			}
			
		}
		updateLabels();
		int k = 0;
		for (int i = 0; i < colBtn.length; i++) {
			if( colBtn[i] == null ) {
				colBtn[i] = new ToolItem(toolBar, SWT.CHECK);
				// colBtn[i].setData(new );
				colBtn[i].addListener(SWT.Selection, e->colSelected(e));
			}
			if( k < colors.size() && colors.get(k) != null ) {
				//colBtn[i].setEnabled(true);
				colBtn[i].setText("");
				colBtn[i].setSelection(selectedColors.contains(colors.get(k)));
				colBtn[i].setData(colors.get(k));
				colBtn[i].setImage(getSquareImage(display, toSwtRGB(colors.get(k))));
				String toolTip = String.format("R: %d\nG: %d\nB: %d", colors.get(k).red,colors.get(k).green,colors.get(k).blue);
				colBtn[i].setToolTipText(toolTip);
				k++;
			} else {
				//if (k < colors.size()) {
					colBtn[i].setText("X");
					colBtn[i].setSelection(false);
					colBtn[i].setData(null);
					if( colBtn[i].getImage()!= null ) colBtn[i].setImage(null);
					k++;
				//}
			}
		}
	}
	
	List<RGB> selectedColors = new ArrayList<>();
	int numOfSelectedColors = 0;
	//Map<RGB,RGB> selectedColors = new HashMap<>();
	
	private void colSelected(Event evt) {
		//System.out.println(item.getData() + " : " + item.getSelection());
		RGB rgb = (RGB) evt.widget.getData();
		boolean selection = ((ToolItem) evt.widget).getSelection();
		if( rgb != null ) {
			if( selection /* && selectedColors.size() < maxColors*/) {
				selectedColors.add(rgb);
				numOfSelectedColors++;
			} else {
				selectedColors.remove(rgb);
				numOfSelectedColors--;
			}
		}
		//disableCol( selectedColors.size() < maxColors );
		updateLabels();
	}

	private void disableCol(boolean b) {
		for (int i = 0; i < colBtn.length; i++) {
			ToolItem j = colBtn[i];
			if( j.getSelection() ) {
				j.setEnabled(true);
			} else {
				j.setEnabled(b);
			}
		}
		
	}

	public void updateLabels() {
		lblSelectedColors.setText(String.format("Selected Colors: %d", numOfSelectedColors));
		lblTotal.setText(String.format("Total: %d", colors.size()));
	}
	
	Map<org.eclipse.swt.graphics.RGB, Image> colImageCache = new HashMap<>();

	private ToolBar toolBar;
	private Label lblSelectedColors;
	private Label lblTotal;
	
	Image getSquareImage(Display display, org.eclipse.swt.graphics.RGB rgb) {
		Image image = colImageCache.get(rgb);
		if (image == null) {
			image = resManager.createImage(ImageDescriptor
					.createFromImage(new Image(display, 12, 12)));
			GC gc = new GC(image);
			Color col = new Color(display, rgb);
			gc.setBackground(col);
			gc.fillRectangle(0, 0, 11, 11);
			Color fg = new Color(display, 0, 0, 0);
			gc.setForeground(fg);
			gc.drawRectangle(0, 0, 11, 11);
			// gc.setBackground(col);
			fg.dispose();
			gc.dispose();
			col.dispose();
			colImageCache.put(rgb, image);
		}
		return image;
	}
	
	public static void main(String[] args)
    {
		PalettePicker dialog = 
//				new CustomMessageBox(new Shell(), 0, SWT.ICON_ERROR,
//						"Warning", "Unsaved changes", "There are unsaved changes in project",
//				new String[]{"", "Cancel", "Proceed"}, 2);
		
		new PalettePicker(new Shell());
		dialog.setColorListProvider(p->getSomeColors(p));
        dialog.open();
    }

	private static List<RGB> getSomeColors(int p) {
		List<RGB> res = new ArrayList<>();		
		Random r = new Random();
		for( int i = 0 ; i<20; i++) {
				res.add( new RGB(r.nextInt(256),r.nextInt(256),r.nextInt(256)));
		}
		return res;
	}

	public void setAccuracy(int colorAccuracy) {
		this.accuracy = colorAccuracy;
	}
	
	public void setMaxNumberOfColors(int numberOfColors) {
		this.maxColors = numberOfColors;
	}

	public int getAccuracy() {
		return accuracy;
	}

	public Collection<RGB> getResult() {
		return result;
	}

}
