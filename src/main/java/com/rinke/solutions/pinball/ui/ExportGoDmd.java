package com.rinke.solutions.pinball.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.Constants;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.AnimationQuantizer;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
public class ExportGoDmd extends Dialog {

	private static final String GODMD_EXPORT_PATH = "godmdExportPath";
	private static final String GODMD_EXPORT_VERSION = "godmdExportVersion";
	protected Pair<String,Integer> result = null;
	protected Shell shlExportForGodmd;
	private Text text;
	private Combo versionCombo;
	
	@Autowired private Config config;
	@Autowired private ViewModel vm;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ExportGoDmd() {
		super(new Shell(), SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.OK | SWT.APPLICATION_MODAL);
		setText("Export for goDMD");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Pair<String,Integer> open() {
		createContents();
		String path = config.get(Config.GODMD_EXPORT_PATH);
		text.setText(path!=null?path:"");
		shlExportForGodmd.open();
		shlExportForGodmd.layout();
		Display display = getParent().getDisplay();
		while (!shlExportForGodmd.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}
	
    // testability overridden by tests
    protected FileChooser createFileChooser(Shell shell, int flags) {   
        return new FileDialogDelegate(shell, flags);
    }

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shlExportForGodmd = new Shell(getParent(), getStyle());
		shlExportForGodmd.setSize(450, 153);
		shlExportForGodmd.setText("Export for goDMD");
		
		Label lblFile = new Label(shlExportForGodmd, SWT.NONE);
		lblFile.setBounds(10, 12, 34, 14);
		lblFile.setText("File:");
		
		text = new Text(shlExportForGodmd, SWT.BORDER);
		text.setBounds(68, 9, 256, 19);
		
		Button btnChoose = new Button(shlExportForGodmd, SWT.NONE);
		btnChoose.setBounds(346, 5, 94, 28);
		btnChoose.setText("Choose");
		btnChoose.addListener(SWT.Selection, e->onChoose());
		
		versionCombo = new Combo(shlExportForGodmd, SWT.READ_ONLY);
		versionCombo.setBounds(68, 34, 122, 22);
		String items[] = { "RGB Version 1", "RGB Version 2", "RGB Version 3" };
		versionCombo.setItems(items);
		versionCombo.select(config.getInteger(GODMD_EXPORT_VERSION, 0));
		
		Label lblVersion = new Label(shlExportForGodmd, SWT.NONE);
		lblVersion.setText("Version:");
		lblVersion.setBounds(10, 38, 52, 14);
		
		Button btnExport = new Button(shlExportForGodmd, SWT.NONE);
		btnExport.setBounds(346, 93, 94, 28);
		btnExport.setText("Export");
		btnExport.addListener(SWT.Selection, e->onExport());
		
		Button btnCancel = new Button(shlExportForGodmd, SWT.NONE);
		btnCancel.setBounds(242, 93, 94, 28);
		btnCancel.setText("Cancel");
		btnCancel.addListener(SWT.Selection, e->onCancel());

	}

	private void onCancel() {
		result = null;
		shlExportForGodmd.close();
	}

	private void onChoose() {
		FileChooser chooser = createFileChooser(shlExportForGodmd, SWT.SAVE);
		chooser.setFileName(text.getText());
		String filename = chooser.open();
		if( filename != null) text.setText(filename);
	}

	private void onExport() {
		config.put(GODMD_EXPORT_VERSION, versionCombo.getSelectionIndex());
		config.put(GODMD_EXPORT_PATH, text.getText());
		result = Pair.of(text.getText(), versionCombo.getSelectionIndex());
		int version = versionCombo.getSelectionIndex()+1;
		List<Animation> toExport = new ArrayList<>();
		
		vm.scenes.values().forEach(p->{
			
			if (p.getActualFrame().planes.size() < Constants.MAX_BIT_PER_COLOR_CHANNEL*3 && p.getPalIndex() <= 8){
				//add animations which are not modified
				toExport.add(p);
			} else {
				//copy ani before export modifications
				Animation exportAni = p.cutScene(0, p.end, p.frames.get(0).planes.size());
				exportAni.setDesc(p.getDesc());
				exportAni.setCycles(p.getCycles());
				exportAni.setHoldCycles(p.getHoldCycles());
				exportAni.setFsk(p.getFsk());
				exportAni.setTransitionsPath(p.getTransitionsPath());
				exportAni.setTransitionName(p.getTransitionName());
				exportAni.setTransitionFrom(p.getTransitionFrom());
				exportAni.setTransitionDelay(p.getTransitionDelay());
				exportAni.setClockFrom(p.getClockFrom());
				exportAni.setClockInFront(p.isClockInFront());
				exportAni.setClockSmall(p.isClockSmall());
				exportAni.setClockWasAdded(p.isClockWasAdded());
				exportAni.setClockXOffset(p.getClockXOffset());
				exportAni.setClockYOffset(p.getClockYOffset());
				
				CompiledAnimation ani = (CompiledAnimation) exportAni;
				ani.actFrame = 0;
				for (int i = 0; i <= ani.end; i++) {
					// remove frame mask
					Frame frame = new Frame(ani.frames.get(i));
					if (frame.hasMask()) {
						frame.mask = null;
					}
					// reduce to RGB24 to 15bit RGB555
					if( frame.planes.size() == Constants.TRUE_COLOR_BIT_PER_CHANNEL*3 ) { // reduce 8 bit per color to 5 bit per color
						frame.planes.remove(0); frame.planes.remove(0); frame.planes.remove(0);
						frame.planes.remove(5); frame.planes.remove(5); frame.planes.remove(5);
						frame.planes.remove(10); frame.planes.remove(10); frame.planes.remove(10);
					}
					// convert to 15bit if it uses a custom palette and version 1 for export
					if (frame.planes.size() < Constants.MAX_BIT_PER_COLOR_CHANNEL*3 && p.getPalIndex() > 8 && version == 1) {
						Palette pal = vm.paletteMap.get(p.getPalIndex());
						AnimationQuantizer quantizer = new AnimationQuantizer();
						Frame qFrame = quantizer.convertFrameToRGB(frame, pal, p.width, p.height, Constants.MAX_BIT_PER_COLOR_CHANNEL);
						frame = qFrame;
						ani.setEditMode(EditMode.REPLACE);
					}
					ani.frames.set(i, frame);
				}
				toExport.add(exportAni);
			}
		});
		
		AniWriter aniWriter = new AniWriter(toExport, text.getText(), version, vm.paletteMap, null);
		if (version >= 3) {
			aniWriter.writeLinearPlane = true;
		}
		aniWriter.run();
		vm.scenes.refresh();
		shlExportForGodmd.close();
	}
}
