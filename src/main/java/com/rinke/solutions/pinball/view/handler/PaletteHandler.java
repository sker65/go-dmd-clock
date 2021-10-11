package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.Constants;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.Dispatcher;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.io.DMCImporter;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.io.PaletteImporter;
import com.rinke.solutions.pinball.io.SmartDMDImporter;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.ui.ConfigDialog;
import com.rinke.solutions.pinball.ui.NamePrompt;
import com.rinke.solutions.pinball.ui.PalettePicker;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.View;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Slf4j
@Bean
public class PaletteHandler extends AbstractCommandHandler implements ViewBindingHandler {
	
	FileHelper fileHelper = new FileHelper();
	@Autowired FileChooserUtil fileChooserUtil;
	@Autowired MessageUtil messageUtil;
	@Autowired PalettePicker palettePicker;
	@Autowired View namePrompt;
	
	@Value(key=Config.COLOR_ACCURACY,defaultValue="0")
    private int colorAccuracy;
	
	private RGB colorBuf0 = null;
	private RGB colorBuf1 = null;
	private RGB colorBuf2 = null;
	private RGB colorBuf3 = null;
	private RGB colorBuf4 = null;
	private RGB colorBuf5 = null;
	private RGB colorBuf6 = null;
	private RGB colorBuf7 = null;
	private RGB colorBuf8 = null;
	private RGB colorBuf9 = null;
	private RGB colorBuf10 = null;
	private RGB colorBuf11 = null;
	private RGB colorBuf12 = null;
	private RGB colorBuf13 = null;
	private RGB colorBuf14 = null;
	private RGB colorBuf15 = null;


	public PaletteHandler(ViewModel vm) {
		super(vm);
	}
	
	public void onRenamePalette(String newName) {
		if (newName.contains(" - ")) {
			vm.selectedPalette.name = newName.split(" - ")[1];
			vm.paletteMap.refresh();
			vm.setDirty(true);
		} else {
			messageUtil.warn("Illegal palette name", "Palette names must consist of palette index and name.\nName format therefore must be '<idx> - <name>'");
			vm.setEditedPaletteName(vm.selectedPalette.index + " - " + vm.selectedPalette.name);
		}
	}
	
	public void onSelectedPaletteTypeChanged(PaletteType o, PaletteType palType) {
		if( vm.selectedPalette != null ) {
			vm.selectedPalette.type = palType;
			// normalize: only one palette can be of type DEFAULT
			if (PaletteType.DEFAULT.equals(palType)) {
				for (Palette p : vm.paletteMap.values()) {
					if (p.index != vm.selectedPalette.index) { // set previous default to
						if (p.type.equals(PaletteType.DEFAULT)) {
							p.type = PaletteType.NORMAL;
						}
					}
				}
			}
			vm.setDirty(true);
		} else {
			log.warn("onSelectedPaletteTypeChanged but no palette selected");
		}
	}
	
	public void onPickColor(int rgb) {
		if( vm.selectedPalette != null && vm.numberOfPlanes >= Constants.MAX_BIT_PER_COLOR_CHANNEL*3) {
			vm.selectedPalette.colors[vm.selectedColor] = RGB.fromInt(rgb);
		} else {
			vm.setSelectedColor(rgb);
		}
		vm.setPaletteDirty(true);
		vm.setDirty(true);
	}

	public void onSelectedPaletteChanged(Palette o, Palette newPalette) {
		if( newPalette != null) {
			log.info("new palette is {}", vm.selectedPalette);
			vm.setSelectedPaletteType(vm.selectedPalette.type);
			//vm.setPaletteDirty(true);  // fix crash on load of small project files
		}
	}
	
	public void onSwapColors(int old, int newIdx) {
		log.info("swapping colors in palette for actual scene: {} -> {}", old, newIdx);
		// TODO should change all scenes that use that palette
		if( vm.selectedScene != null ) {
			CompiledAnimation ani = vm.selectedScene;
			for( Frame f : ani.frames ) {
				swap(f, old, newIdx, vm.dmd.getWidth(), vm.dmd.getHeight());
			}
			swap(vm.dmd.getFrame(), old, newIdx, vm.dmd.getWidth(), vm.dmd.getHeight());
			vm.setPaletteDirty(true);
			vm.setDirty(true);
		}
	}
	
	public void onPickPalette() {
		if( vm.selectedScene != null ) {
			palettePicker.setAccuracy(colorAccuracy);
			palettePicker.setColorListProvider(p->extractColorsFromScene(vm.selectedScene, p));
			palettePicker.open();
			colorAccuracy = palettePicker.getAccuracy();
			if( palettePicker.getResult() != null && vm.selectedPalette != null ) {
				int i = 0;
				for( RGB c : palettePicker.getResult()) {
					if( i < vm.selectedPalette.numberOfColors ) vm.selectedPalette.colors[i++] = c;
				}
				vm.setPaletteDirty(true);
			}
		}
	}
	
	
	int revertGamma(int val){

		int gamma8[] = {
			0, 0, 0, 0 , 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2,
			2, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5,
			5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10,
			10, 10, 11, 11, 11, 12, 12, 13, 13, 13, 14, 14, 15, 15, 16, 16,
			17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 24, 24, 25,
			25, 26, 27, 27, 28, 29, 29, 30, 31, 32, 32, 33, 34, 35, 35, 36,
			37, 38, 39, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 50,
			51, 52, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 66, 67, 68,
			69, 70, 72, 73, 74, 75, 77, 78, 79, 81, 82, 83, 85, 86, 87, 89,
			90, 92, 93, 95, 96, 98, 99, 101, 102, 104, 105, 107, 109, 110, 112, 114,
			115, 117, 119, 120, 122, 124, 126, 127, 129, 131, 133, 135, 137, 138, 140, 142,
			144, 146, 148, 150, 152, 154, 156, 158, 160, 162, 164, 167, 169, 171, 173, 175,
			177, 180, 182, 184, 186, 189, 191, 193, 196, 198, 200, 203, 205, 208, 210, 213,
			215, 218, 220, 223, 225, 228, 231, 233, 236, 239, 241, 244, 247, 249, 252, 255 };

		for (int retval = 0; retval < 256; retval++){
			if (gamma8[retval] >= val)
				return retval;
		}
		return 0;

	}
	
	public void onColorCorrectPalette() {

		int res = messageUtil.warn(0, "Warning",
				"Use only on old projects !", 
				"This function corrects the palette color values of old projects",
				new String[]{"", "Cancel", "Proceed"},2);
		if( res != 2 ) return;
		for (Palette pal : vm.paletteMap.values()) {
			for(int i = 0; i < vm.getNumberOfColors(); i++ ) {
				RGB col = pal.colors[i];
				col.red = revertGamma(col.red);
				col.green = revertGamma(col.green);;
				col.blue = revertGamma(col.blue);;
			}
		}
		vm.setPaletteDirty(true);
		if (vm.selectedRecording != null || vm.selectedScene != null) {
			vm.setDmdDirty(true);
		}
		vm.setDirty(true);
	}
	
	List<RGB> extractColorsFromScene(CompiledAnimation scene, int accuracy) {
		int w = scene.width;
		int h = scene.height;
		List<RGB> result = new ArrayList<>();
		for( Frame f: scene.frames) {
			for( int x = 0; x < w; x++) {
				for(int y=0; y < h; y++) {
					int bytesPerRow = w / 8;
			    	byte mask = (byte) (0b10000000 >> (x % 8));
			    	int rgb = 0;
			    	for(int plane = 0; plane < f.planes.size(); plane++) {
			    		rgb += (f.planes.get(plane).data[x / 8 + y * bytesPerRow] & mask) != 0 ? (1<<plane) : 0;
			    	}
			    	// color
			    	RGB col = RGB.fromInt(rgb);
			    	if( !inList( result, col, accuracy ) ) {
			    		result.add(col);
			    	}
				}
			}
		}
		return result;
	}
	
	/**
	 * checks if a given color is already in the list with respect to the given accuracy
	 * @param colors list of colors
	 * @param col color to check
	 * @param accuracy accuracy from 0 to 100 (100 very accurate -> exact match)
	 * @return true if similar color is found
	 */
	private boolean inList(List<RGB> colors, RGB col, int accuracy) {
		for( RGB c : colors) {
			if (c != null) {
				float d = getColorDistance(c, col); 
				//System.out.println(d);
				if( d < (0.001f + 2f * accuracy) ) return true; 
			}
		}
		return false;
	}
	
	public float getColorDistance(RGB c1, RGB c2) {
		return (float) getColorDistance(c1.red, c1.green, c1.blue, c2.red, c2.green, c2.blue);
	}

	public double getColorDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
		return Math.sqrt(Math.pow(r2 - r1, 2) + Math.pow(g2 - g1, 2) + Math.pow(b2 - b1, 2));
	}

	// TODO sehr ähnlich funktionen gibts im Animation Quantisierer
	/**
	 * swap color index from old to new
	 * @param f 
	 * @param o old color index
	 * @param n new color index
	 */
	public void swap(Frame f, int o, int n, int w, int h) {
		List<Plane> np = new ArrayList<>();
		// copy planes
		for(int plane = 0; plane < f.planes.size(); plane++) {
			np.add( new Plane(f.planes.get(plane)));
		}
		// transform (swap) planes
		for( int x = 0; x < w; x++) {
			for(int y=0; y < h; y++) {
				int bytesPerRow = w / 8;
		    	byte mask = (byte) (0b10000000 >> (x % 8));
		    	int v = 0;
		    	int nv = 0;
		    	for(int plane = 0; plane < f.planes.size(); plane++) {
		    		v += (f.planes.get(plane).data[x / 8 + y * bytesPerRow] & mask) != 0 ? (1<<plane) : 0;
		    	}
		    	// swap
		    	if( v == o ) nv = n;
		    	else if( v == n ) nv = o;
		    	else nv = v;
		    	
		    	for(int plane = 0; plane < np.size(); plane++) {
		    		if( ((1<<plane)) != 0) {
		        		if( (nv & 0x01) != 0) {
		        			np.get(plane).data[y*bytesPerRow+x/8] |= mask;
		        		} else {
		        			np.get(plane).data[y*bytesPerRow+x/8] &= ~mask;
		        		}
		    		}
		    		nv >>= 1;
		    	}		    	
			} // y
		} // x
		// replace it
		f.planes = np;
	}
	
	public void onExtractPalColorsFromFrame() {
		int i = 0;
		palettePicker.setAccuracy(colorAccuracy);
		palettePicker.setMaxNumberOfColors(vm.numberOfColors);
		if (vm.dmd.getNumberOfPlanes() == 24 && vm.previewDMD != null && (vm.previewDMD.getNumberOfPlanes() == 2 || vm.previewDMD.getNumberOfPlanes() == 4)) {
			palettePicker.setColorListProvider(p->extractColorsFromFrameAndSort(vm.dmd, vm.previewDMD, p));
			i = vm.getSelectedColor(); 
			}
		else {
			palettePicker.setColorListProvider(p->extractColorsFromFrame(vm.dmd, p));
			}
		palettePicker.open();
		colorAccuracy = palettePicker.getAccuracy();
		if( palettePicker.getResult() != null && vm.selectedPalette != null ) {
			for( RGB c : palettePicker.getResult()) {
				if (c == null) {
					i++;
				} else {
					if( i < vm.selectedPalette.numberOfColors ) vm.selectedPalette.colors[i++] = c;
				}
			}
			vm.setPaletteDirty(true);
		}
	}
	
	List<RGB> extractColorsFromFrameAndSort (DMD dmd, DMD previewDMD, int accuracy) {
		List<RGB> res = new ArrayList<>();
		List<RGB> sortedRes = new ArrayList<>();
		int numberOfShades = (int) Math.pow(2,previewDMD.getNumberOfPlanes());
		for (int i = 0; i < numberOfShades; i++) {
			for( int x = 0; x < previewDMD.getWidth(); x++) {
				for(int y = 0; y < previewDMD.getHeight(); y++) {
					if (i == previewDMD.getPixelWithoutMask(x, y)) {
						int rgb = dmd.getPixelWithoutMask(x, y);
						RGB col = RGB.fromInt(rgb);
						if( !inList(res, col, accuracy) ) {
							res.add(col);
						}
					}
				}
			}
			res.add(null);
		}
		
		return res;
	}


	List<RGB> extractColorsFromFrame(DMD dmd, int accuracy) {
		List<RGB> res = new ArrayList<>();
		for( int x = 0; x < dmd.getWidth(); x++) {
			for(int y = 0; y < dmd.getHeight(); y++) {
				int rgb = dmd.getPixelWithoutMask(x, y);
				RGB col = RGB.fromInt(rgb);
				if( !inList(res, col, accuracy) ) {
					res.add(col);
				}
			}
		}
		return res;
	}

	private boolean isNewPaletteName(String text) {
		for (Palette pal : vm.paletteMap.values()) {
			if (pal.name.equals(text))
				return false;
		}
		return true;
	}
	
	public void copyPalettePlaneUpgrade(String hint) {
		String name = vm.selectedPalette.name;
		if( hint != null ) name = hint;
		if (!isNewPaletteName(name)) {
			name = "new" + UUID.randomUUID().toString().substring(0, 4);
		}
		
		RGB[] actCols = vm.selectedPalette.colors;
		RGB[] cols = new RGB[actCols.length];
		// copy
		for( int i = 0; i< cols.length; i++) cols[i] = RGB.of(actCols[i]);
		
		cols[2] = RGB.of(actCols[4]);
		cols[3] = RGB.of(actCols[15]);
		
		Palette newPalette = new Palette(cols, vm.paletteMap.size(), name);
		for( Palette pal : vm.paletteMap.values() ) {
			if( pal.sameColors(cols)) {
				vm.setSelectedPalette(pal);
				return;
			}
		}
		
		vm.paletteMap.put(newPalette.index, newPalette);
		vm.setSelectedPalette(newPalette);
	}
	
	public void onNewPalette() {
		if (vm.livePreviewActive ) {
			vm.setLivePreviewActive(false);
		}
		
		String name = "pal " + UUID.randomUUID().toString().substring(0, 4);
		
		NamePrompt namePrompt = (NamePrompt) this.namePrompt;
		namePrompt.setItemName("Palette");
		namePrompt.setPrompt(name);
		namePrompt.open();
		if( namePrompt.isOkay() ) name = namePrompt.getPrompt();
		else return;
		
		//Palette newPal =  new Palette(vm.selectedPalette.colors, getHighestIndex(vm.paletteMap)+1, name);
		Palette newPal =  new Palette(vm.selectedPalette.colors, getNextFreeIndex(), name);
		vm.paletteMap.put(newPal.index,newPal);
		vm.setSelectedPalette(newPal);
		vm.setSelectedColor(0);
		vm.setPaletteDirty(true);
		vm.setDirty(true);
	}

	private int getHighestIndex(Map<Integer, Palette> paletteMap) {
		OptionalInt max = paletteMap.values().stream().mapToInt(p->p.index).max();
		return max.orElse(0);
	}
	
	private int getNextFreeIndex() {
		int i = 0;
		boolean exists = false;
		do {
			exists = false;			
			for (Palette p : vm.paletteMap.values()) {
				if (p.index == i) {
					exists = true;
					break;
				}
			}
			i++;
		} while ((exists == true) && (i < 256));
		return i-1;
	}

	public void onSavePalette() {
		String filename = fileChooserUtil.choose(SWT.SAVE, vm.selectedPalette.name, new String[] { "*.xml", "*.json" }, new String[] { "Paletten XML", "Paletten JSON" });
		if (filename != null) {
			log.info("store palette to {}", filename);
			fileHelper.storeObject(vm.selectedPalette, filename);
		}
	}

	public void onLoadPalette() {
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.xml", "*.json,", "*.txt", "*.dmc" }, new String[] { "Palette XML",
				"Palette JSON", "smartdmd", "DMC" });
		if (filename != null)
			loadPalette(filename);
	}

	void loadPalette(String filename) {
		java.util.List<Palette> palettesImported = null;
		if (filename.toLowerCase().endsWith(".txt") || filename.toLowerCase().endsWith(".dmc")) {
			palettesImported = getImporterByFilename(filename).importFromFile(filename);
		} else {
			Palette pal = (Palette) fileHelper.loadObject(filename);
			log.info("load palette from {}", filename);
			palettesImported = Arrays.asList(pal);
		}
		if( palettesImported != null ) {
			String overwrite = checkOverwrite(vm.paletteMap, palettesImported);
			if (!overwrite.isEmpty()) {
				int res = messageUtil.warn(0,"Warning",
						"Palette conflict", "The following palettes already exist: " + overwrite + "\n", new String[]{"Cancel", "Replace", "Append"},2);
				if (res == 1) 
					importPalettes(palettesImported, true);
				else if (res == 2)	
					importPalettes(palettesImported, false);
				else
					return;
			} else {
				importPalettes(palettesImported, true);
			}
			//editor.v.recentPalettesMenuManager.populateRecent(filename);
			// view needs to listen / Custom Binding
			vm.setRecentPalette(filename);
		}
	}

	private PaletteImporter getImporterByFilename(String filename) {
		if (filename.toLowerCase().endsWith(".txt")) {
			return new SmartDMDImporter();
		} else if (filename.toLowerCase().endsWith(".dmc")) {
			return new DMCImporter();
		}
		return null;
	}
	
	void importPalettes(java.util.List<Palette> palettesImported, boolean overwrite) {
		for (Palette p : palettesImported) {
			if (vm.paletteMap.containsKey(p.index)) {
				if (overwrite) {
					vm.paletteMap.put(p.index, p);
				} else {
					vm.paletteMap.put(p.index = getNextFreeIndex(), p);
				} 
					
			} else {
				vm.paletteMap.put(p.index, p);
			}
		}
	}

	String checkOverwrite(java.util.Map<Integer,Palette> pm, java.util.List<Palette> palettesImported) {
		StringBuilder sb = new StringBuilder();
		for (Palette pi : palettesImported) {
			if (pm.containsKey(pi.index)) {
				sb.append(pi.index + ", ");
			}
		}
		return sb.toString();
	}
	
	public void onCreateGradients() {
		if( vm.selectedPalette != null) {
			int colorIndex = vm.getSelectedColor() | 3; // select last color of group.
			RGB color = RGB.of( vm.selectedPalette.colors[colorIndex]);
			RGB color66 = RGB.of( vm.selectedPalette.colors[colorIndex - 1] );
            RGB color33 = RGB.of( vm.selectedPalette.colors[colorIndex - 2] );
			RGB color0 = RGB.of( vm.selectedPalette.colors[colorIndex - 3]);
			
			color66.blue = (color0.blue / 3) + ((color.blue / 3) * 2);
			color66.red = (color0.red / 3) + ((color.red / 3) * 2);
			color66.green = (color0.green / 3) + ((color.green / 3) * 2);
			
			color33.blue = ((color0.blue / 3) * 2) + (color.blue / 3);
			color33.red = ((color0.red / 3) * 2) + (color.red / 3);
			color33.green = ((color0.green / 3) * 2) + (color.green / 3);

			vm.selectedPalette.colors[colorIndex-1] = RGB.of(color66);
			vm.selectedPalette.colors[colorIndex-2] = RGB.of(color33);
			vm.setSelectedColor(colorIndex);
			vm.setPaletteDirty(true);
			if (vm.selectedRecording != null || vm.selectedScene != null) {
				vm.setDmdDirty(true);
			}
			vm.setDirty(true);
		}
	}
	
	public void onCreateGradients16() {
		if( vm.selectedPalette != null) {
			int colorIndex = vm.getSelectedColor() | 15; // select last color of group.
			RGB color15 = RGB.of( vm.selectedPalette.colors[colorIndex]);
			RGB color0 = RGB.of( vm.selectedPalette.colors[colorIndex - 15]);
			
			for (int i = 1; i < 15; i++) {
				RGB color = RGB.of( vm.selectedPalette.colors[colorIndex - i] );
				color.blue = ((color0.blue / 15) * i) + ((color15.blue / 15) * (15-i));
				color.red = ((color0.red / 15) * i) + ((color15.red / 15) * (15-i));
				color.green = ((color0.green / 15) * i) + ((color15.green / 15) * (15-i));	
				vm.selectedPalette.colors[colorIndex-i] = RGB.of(color);
			}

			vm.setSelectedColor(colorIndex);
			vm.setPaletteDirty(true);
			if (vm.selectedRecording != null || vm.selectedScene != null) {
				vm.setDmdDirty(true);
			}
			vm.setDirty(true);
		}
	}

	public void onCopySwatch() {
		if( vm.selectedPalette != null) {
			int colorIndex = vm.getSelectedColor() | 3; // select last color of group.
			colorBuf0 = RGB.of(vm.selectedPalette.colors[colorIndex]);
			colorBuf1 = RGB.of(vm.selectedPalette.colors[colorIndex - 1]);
			colorBuf2 = RGB.of(vm.selectedPalette.colors[colorIndex - 2]);
			colorBuf3 = RGB.of(vm.selectedPalette.colors[colorIndex - 3]);
		}
	}

	public void onCopySwatch16() {
		if( vm.selectedPalette != null) {
			int colorIndex = vm.getSelectedColor() | 15; // select last color of group.
			colorBuf0 = RGB.of(vm.selectedPalette.colors[colorIndex]);
			colorBuf1 = RGB.of(vm.selectedPalette.colors[colorIndex - 1]);
			colorBuf2 = RGB.of(vm.selectedPalette.colors[colorIndex - 2]);
			colorBuf3 = RGB.of(vm.selectedPalette.colors[colorIndex - 3]);
			colorBuf4 = RGB.of(vm.selectedPalette.colors[colorIndex - 4]);
			colorBuf5 = RGB.of(vm.selectedPalette.colors[colorIndex - 5]);
			colorBuf6 = RGB.of(vm.selectedPalette.colors[colorIndex - 6]);
			colorBuf7 = RGB.of(vm.selectedPalette.colors[colorIndex - 7]);
			colorBuf8 = RGB.of(vm.selectedPalette.colors[colorIndex - 8]);
			colorBuf9 = RGB.of(vm.selectedPalette.colors[colorIndex - 9]);
			colorBuf10 = RGB.of(vm.selectedPalette.colors[colorIndex - 10]);
			colorBuf11 = RGB.of(vm.selectedPalette.colors[colorIndex - 11]);
			colorBuf12 = RGB.of(vm.selectedPalette.colors[colorIndex - 12]);
			colorBuf13 = RGB.of(vm.selectedPalette.colors[colorIndex - 13]);
			colorBuf14 = RGB.of(vm.selectedPalette.colors[colorIndex - 14]);
			colorBuf15 = RGB.of(vm.selectedPalette.colors[colorIndex - 15]);
		}
	}
	
	public void onPasteSwatch() {
		if( vm.selectedPalette != null && colorBuf0 != null) {
			int colorIndex = vm.getSelectedColor() | 3; // select last color of group.
			vm.selectedPalette.colors[colorIndex] = RGB.of(colorBuf0);
			vm.selectedPalette.colors[colorIndex - 1] = RGB.of(colorBuf1);
			vm.selectedPalette.colors[colorIndex - 2] = RGB.of(colorBuf2);
			vm.selectedPalette.colors[colorIndex - 3] = RGB.of(colorBuf3);

			vm.setPaletteDirty(true);
			if (vm.selectedRecording != null || vm.selectedScene != null) {
				vm.setDmdDirty(true);
			}
			vm.setDirty(true);
		}
	}

	public void onPasteSwatch16() {
		if( vm.selectedPalette != null && colorBuf0 != null) {
			int colorIndex = vm.getSelectedColor() | 15; // select last color of group.
			vm.selectedPalette.colors[colorIndex] = RGB.of(colorBuf0);
			vm.selectedPalette.colors[colorIndex - 1] = RGB.of(colorBuf1);
			vm.selectedPalette.colors[colorIndex - 2] = RGB.of(colorBuf2);
			vm.selectedPalette.colors[colorIndex - 3] = RGB.of(colorBuf3);
			vm.selectedPalette.colors[colorIndex - 4] = RGB.of(colorBuf4);
			vm.selectedPalette.colors[colorIndex - 5] = RGB.of(colorBuf5);
			vm.selectedPalette.colors[colorIndex - 6] = RGB.of(colorBuf6);
			vm.selectedPalette.colors[colorIndex - 7] = RGB.of(colorBuf7);
			vm.selectedPalette.colors[colorIndex - 8] = RGB.of(colorBuf8);
			vm.selectedPalette.colors[colorIndex - 9] = RGB.of(colorBuf9);
			vm.selectedPalette.colors[colorIndex - 10] = RGB.of(colorBuf10);
			vm.selectedPalette.colors[colorIndex - 11] = RGB.of(colorBuf11);
			vm.selectedPalette.colors[colorIndex - 12] = RGB.of(colorBuf12);
			vm.selectedPalette.colors[colorIndex - 13] = RGB.of(colorBuf13);
			vm.selectedPalette.colors[colorIndex - 14] = RGB.of(colorBuf14);
			vm.selectedPalette.colors[colorIndex - 15] = RGB.of(colorBuf15);

			vm.setPaletteDirty(true);
			if (vm.selectedRecording != null || vm.selectedScene != null) {
				vm.setDmdDirty(true);
			}
			vm.setDirty(true);
		}
	}
	
	
	public void onSwapSwatch() {
		if( vm.selectedPalette != null && colorBuf0 != null) {
			int colorIndex = vm.getSelectedColor() | 3; // select last color of group.
			RGB color0 = RGB.of(vm.selectedPalette.colors[colorIndex]);
			RGB color1 = RGB.of(vm.selectedPalette.colors[colorIndex - 1]);
			RGB color2 = RGB.of(vm.selectedPalette.colors[colorIndex - 2]);
			RGB color3 = RGB.of(vm.selectedPalette.colors[colorIndex - 3]);
			
			vm.selectedPalette.colors[colorIndex] = RGB.of(colorBuf0);
			vm.selectedPalette.colors[colorIndex - 1] = RGB.of(colorBuf1);
			vm.selectedPalette.colors[colorIndex - 2] = RGB.of(colorBuf2);
			vm.selectedPalette.colors[colorIndex - 3] = RGB.of(colorBuf3);

			colorBuf0 = RGB.of(color0);
			colorBuf1 = RGB.of(color1);
			colorBuf2 = RGB.of(color2);
			colorBuf3 = RGB.of(color3);
			
			vm.setPaletteDirty(true);
			if (vm.selectedRecording != null || vm.selectedScene != null) {
				vm.setDmdDirty(true);
			}
			vm.setDirty(true);
		}
	}

	public void onSwapSwatch16() {
		int colorIndex = vm.getSelectedColor() | 15; // select last color of group.
		if( vm.selectedPalette != null && colorBuf0 != null) {
			RGB color0 = RGB.of(vm.selectedPalette.colors[colorIndex]);
			RGB color1 = RGB.of(vm.selectedPalette.colors[colorIndex - 1]);
			RGB color2 = RGB.of(vm.selectedPalette.colors[colorIndex - 2]);
			RGB color3 = RGB.of(vm.selectedPalette.colors[colorIndex - 3]);
			RGB color4 = RGB.of(vm.selectedPalette.colors[colorIndex - 4]);
			RGB color5 = RGB.of(vm.selectedPalette.colors[colorIndex - 5]);
			RGB color6 = RGB.of(vm.selectedPalette.colors[colorIndex - 6]);
			RGB color7 = RGB.of(vm.selectedPalette.colors[colorIndex - 7]);
			RGB color8 = RGB.of(vm.selectedPalette.colors[colorIndex - 8]);
			RGB color9 = RGB.of(vm.selectedPalette.colors[colorIndex - 9]);
			RGB color10 = RGB.of(vm.selectedPalette.colors[colorIndex - 10]);
			RGB color11 = RGB.of(vm.selectedPalette.colors[colorIndex - 11]);
			RGB color12 = RGB.of(vm.selectedPalette.colors[colorIndex - 12]);
			RGB color13 = RGB.of(vm.selectedPalette.colors[colorIndex - 13]);
			RGB color14 = RGB.of(vm.selectedPalette.colors[colorIndex - 14]);
			RGB color15 = RGB.of(vm.selectedPalette.colors[colorIndex - 15]);
			
			vm.selectedPalette.colors[colorIndex] = RGB.of(colorBuf0);
			vm.selectedPalette.colors[colorIndex - 1] = RGB.of(colorBuf1);
			vm.selectedPalette.colors[colorIndex - 2] = RGB.of(colorBuf2);
			vm.selectedPalette.colors[colorIndex - 3] = RGB.of(colorBuf3);
			vm.selectedPalette.colors[colorIndex - 4] = RGB.of(colorBuf4);
			vm.selectedPalette.colors[colorIndex - 5] = RGB.of(colorBuf5);
			vm.selectedPalette.colors[colorIndex - 6] = RGB.of(colorBuf6);
			vm.selectedPalette.colors[colorIndex - 7] = RGB.of(colorBuf7);
			vm.selectedPalette.colors[colorIndex - 8] = RGB.of(colorBuf8);
			vm.selectedPalette.colors[colorIndex - 9] = RGB.of(colorBuf9);
			vm.selectedPalette.colors[colorIndex - 10] = RGB.of(colorBuf10);
			vm.selectedPalette.colors[colorIndex - 11] = RGB.of(colorBuf11);
			vm.selectedPalette.colors[colorIndex - 12] = RGB.of(colorBuf12);
			vm.selectedPalette.colors[colorIndex - 13] = RGB.of(colorBuf13);
			vm.selectedPalette.colors[colorIndex - 14] = RGB.of(colorBuf14);
			vm.selectedPalette.colors[colorIndex - 15] = RGB.of(colorBuf15);

			colorBuf0 = RGB.of(color0);
			colorBuf1 = RGB.of(color1);
			colorBuf2 = RGB.of(color2);
			colorBuf3 = RGB.of(color3);
			colorBuf4 = RGB.of(color4);
			colorBuf5 = RGB.of(color5);
			colorBuf6 = RGB.of(color6);
			colorBuf7 = RGB.of(color7);
			colorBuf8 = RGB.of(color8);
			colorBuf9 = RGB.of(color9);
			colorBuf10 = RGB.of(color10);
			colorBuf11 = RGB.of(color11);
			colorBuf12 = RGB.of(color12);
			colorBuf13 = RGB.of(color13);
			colorBuf14 = RGB.of(color14);
			colorBuf15 = RGB.of(color15);
			
			vm.setPaletteDirty(true);
			if (vm.selectedRecording != null || vm.selectedScene != null) {
				vm.setDmdDirty(true);
			}
			vm.setDirty(true);
		}
	}

	public void onDeleteUnusedPalettes() {
		int size = vm.paletteMap.size();
		for (int i = 0; i < size; i++) {
			Palette p = vm.paletteMap.get(i);
			if( p != null )
				if( p.type != PaletteType.DEFAULT ) {
					// check if any scene is using this
					boolean sceneFound = false;
					for( Animation a: vm.scenes.values()) {
						if( a.getPalIndex() == p.index ) {
							if( !sceneFound ) {
								sceneFound = true;
							}
						}
					}
					// check if any keyframe is using this
					boolean keyFrameFound = false;
					for( PalMapping pm : vm.keyframes.values()) {
						if( pm.palIndex == p.index ) {
							if( !keyFrameFound ) {
								keyFrameFound = true;
							}
						}
					}
					if( sceneFound == false && keyFrameFound == false && p.index != 0) {
						vm.paletteMap.remove(p.index);
					}
				} else {
					vm.setSelectedPalette(p);
				}
		}
		
	}

	public void onDeletePalette() {
		if( vm.selectedPalette != null && vm.paletteMap.size()>1 ) {
			// check if any scene is using this
			List<String> res = new ArrayList<>();
			boolean sceneFound = false;
			for( Animation a: vm.scenes.values()) {
				if( a.getPalIndex() == vm.selectedPalette.index ) {
					if( !sceneFound ) {
						res.add("Scenes:\n");
						sceneFound = true;
					}
					res.add(a.getDesc());
				}
			}
			//res.add("\n\nRecordings:\n");
			//for( Animation a: vm.recordings.values()) {
			//	if( a.getPalIndex() == vm.selectedPalette.index ) {
			//		res.add(a.getDesc());
			//	}
			//}
			
			// also check keyframes
			boolean keyFrameFound = false;
			for( PalMapping pm : vm.keyframes.values()) {
				if( pm.palIndex == vm.selectedPalette.index ) {
					if( !keyFrameFound ) {
						res.add("\n\nKeyframes:\n");
						keyFrameFound = true;
					}
					res.add(pm.name);
				}
			}
			if( res.isEmpty() ) {
				if (vm.selectedPalette.index == 0)
					messageUtil.warn("Error","Palette 0 cannot be deleted");
				else {
					vm.paletteMap.remove(vm.selectedPalette.index);
					// ensure there is a default palette
					int c = 0;
					for( Palette p : vm.paletteMap.values()) {
						if( p.type == PaletteType.DEFAULT ) c++;
					}
					if( c == 0 ) vm.paletteMap.get(0).type = PaletteType.DEFAULT;
					// select first
					vm.setSelectedPalette(vm.paletteMap.values().iterator().next());
				}
			} else {
				messageUtil.warn("Palette cannot be deleted", "It is used by the following resources: \n"+res);
				Clipboard clipboard=new Clipboard(Display.getCurrent());
				TextTransfer transfer=TextTransfer.getInstance();
				clipboard.setContents(new Object[]{res.toString()},new Transfer[]{transfer});
				clipboard.dispose();
			}
		}
	}

}
