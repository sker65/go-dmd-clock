package com.rinke.solutions.pinball;

import java.awt.SplashScreen;
import java.io.File;

import org.eclipse.core.runtime.Status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Scene;
import com.rinke.solutions.pinball.widget.DMDWidget;
import com.rinke.solutions.pinball.widget.DrawTool;
import com.rinke.solutions.pinball.widget.SetPixelTool;
import com.rinke.solutions.pinball.widget.FloodFillTool;
import com.rinke.solutions.pinball.widget.RectTool;
import com.rinke.solutions.pinball.ui.About;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.wb.swt.SWTResourceManager;


public class PinDmdEditor {

	/**
	 * handles redraw of animations
	 * @author steve
	 */
    private class CyclicRedraw implements Runnable {

		@Override
		public void run() {
			previewCanvas.redraw();
			if( dmdWidget!=null) dmdWidget.redraw();
            if (animationHandler != null) {
            	animationHandler.run();
                display.timerExec(animationHandler.getRefreshDelay(), cyclicRedraw);
            }
		}
    }
	
	private static class PaletteViewerLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return super.getImage(element);
		}
		public String getText(Object element) {
			Palette pal = ((Palette)element);
			return pal.index + " - " + pal.name;
		}
	}
	
	private static class ViewerLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return super.getImage(element);
		}
		public String getText(Object element) {
			return ((PalMapping) element).name;
		}
	}

	private static class AniViewerLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return super.getImage(element);
		}
		public String getText(Object element) {
			return ((Animation) element).getDesc();
		}
	}

	DMD dmd = new DMD(128,32);
	AnimationHandler animationHandler = null;
	CyclicRedraw cyclicRedraw = null;
	
	Project project = new Project();
	java.util.List<Animation> animations = new ArrayList<>();
    Map<String,DrawTool> drawTools = new HashMap<>();

	protected Shell shlPindmdEditor;
	private Text txtDuration;
	protected long lastTimeCode;
    Display display;
    final ToolItem colBtn[] = new ToolItem[16];
	private Canvas previewCanvas;
	private Label lblTcval;
	private Label lblFrameNo;
	private Scale scale;


	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			PinDmdEditor window = new PinDmdEditor();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    java.util.List<byte[]> hashes = new ArrayList<byte[]>();

    private void saveHashes(java.util.List<byte[]> hashes) {
        if( hashes != null ) {
            this.hashes.clear();
            for( byte[] h : hashes) {
                this.hashes.add( Arrays.copyOf(h, h.length));
            }
        }
    }

	/**
	 * Open the window.
	 */
	public void open() {
		display = Display.getDefault();
		
		project.palettes.add(new Palette(Palette.defaultColors(), 0, "default"));
		
		cyclicRedraw = new CyclicRedraw();

		createContents();
		
		paletteComboViewer.getCombo().select(0);
		
		animationHandler = new AnimationHandler(playingAnis, clock, dmd, previewCanvas, false);
        animationHandler.setScale(scale);
		animationHandler.setLabelHandler(new EventHandler() {
		    
            @Override
            public void notifyAni(AniEvent evt) {
                switch (evt.evtType) {
                case ANI:
                    lblFrameNo.setText(""+ evt.actFrame);
                    lblTcval.setText( ""+evt.timecode);
                    //hashLabel.setText(
                    int i = 0;
                    for( byte[] p : evt.hashes) {
						btnHash[i++].setText(getPrintableHashes(p));
					}

                    saveHashes(evt.hashes);
                    lastTimeCode = evt.timecode;
                    break;
                case CLOCK:
                    lblFrameNo.setText("");
                    lblTcval.setText("");
                    // sourceList.deselectAll();
                    break;
                }
            }

        });
		
		SplashScreen splashScreen = SplashScreen.getSplashScreen();
		if( splashScreen!=null) {
		    splashScreen.close();
		}

        shell = shlPindmdEditor.getShell();
		shlPindmdEditor.open();
		shlPindmdEditor.layout();

		GlobalExceptionHandler.getInstance().setDisplay(display);
        GlobalExceptionHandler.getInstance().setShell(shell);

		display.timerExec(animationHandler.getRefreshDelay(), cyclicRedraw);
		
		loadAni("./drwho-dump.txt.gz", false, true);
		
        int retry = 0;
        while (true ) {
            try {
                LOG.info("entering event loop");
                while (!shell.isDisposed()) {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                System.exit(0);
            } catch( Exception e) {
                GlobalExceptionHandler.getInstance().showError(e);
                LOG.error("unexpected error: {}",e);
                if (retry++ > 10)
                    System.exit(1);
            }
        }

	}
	
    private DMDClock clock = new DMDClock(false);

	Shell shell;
	String lastPath;
    private static Logger LOG = LoggerFactory.getLogger(PinDmdEditor.class); 
    FileHelper fileHelper = new FileHelper();
	private String frameTextPrefix = "Pin2dmd Editor ";
	private Animation selectedAnimation = null;
	private int selectedAnimationIndex = 0;
	private java.util.List<Animation> playingAnis = new ArrayList<Animation>();
	private int activePaletteIndex;
	
    private Object loadProject(Event e) {
        FileDialog fileChooser = new FileDialog(shell, SWT.OPEN);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml;*.json;" });
        fileChooser.setFilterNames(new String[] { "Project XML", "Project JSON" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename != null) {
            LOG.info("load project from {}",filename);
            Project projectToLoad  = (Project) fileHelper.loadObject(filename);

            if( projectToLoad != null ) {
                shell.setText(frameTextPrefix+" - "+project.inputFile);
                project = projectToLoad;
                
                //loadAni(project.inputFile, false, false);
                for( int i = 1; i < project.scenes.size(); i++) {
                	//cutOutNewAnimation(project.scenes.get(i).start, project.scenes.get(i).end, animations.get(0));
                	System.out.println("cutting out "+project.scenes.get(i));
                }
                project.palettes = project.palettes;
                project.palMappings = project.palMappings;
                
            }
        }
        
        // TODO recreate animation list on load of project
        // which means reload initial source file and recreate
        // scenes and populate source list
        
        return null;
    }

    private void saveProject(Event e) {
        FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
        fileChooser.setOverwrite(true);
        //fileChooser.setFileName(project.name);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml", "*.json", "*.dat" });
        fileChooser.setFilterNames(new String[] { "Project XML", "Project JSON", "Export dat" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();        
        if (filename != null) {
            LOG.info("write project to {}",filename);
            fileHelper.storeObject(project, filename);
         }
    }

    protected void loadAniWithFC(boolean append) {
        FileDialog fileChooser = new FileDialog(shell, SWT.OPEN);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.properties;*.ani;*.txt.gz;*.pcap;*.pcap.gz" });
        fileChooser.setFilterNames(new String[] { "Animationen", "properties, txt.gz, ani" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename == null)
            return;

        loadAni(filename, append, true);
    }
    
    protected void loadAni(String filename, boolean append, boolean populateProject) {
        java.util.List<Animation> loadedList = new ArrayList<>();
        if (filename.endsWith(".ani")) {
            loadedList.addAll(AnimationCompiler.readFromCompiledFile(filename));
        } else if (filename.endsWith(".txt.gz")) {
            loadedList.add(buildAnimationFromFile(filename, AnimationType.MAME));
        } else if (filename.endsWith(".properties")) {
            loadedList.addAll(AnimationFactory.createAnimationsFromProperties(filename));
        } else if (filename.endsWith(".pcap") || filename.endsWith(".pcap.gz") ) {
        	loadedList.add(buildAnimationFromFile(filename, AnimationType.PCAP));
        }
        
        if( populateProject ) {
            project.inputFile = filename;
            //DMD dmd = new DMD(128, 32);
            for (Animation ani : loadedList) {
    			project.scenes.add(new Scene(ani.getDesc(),0,/*ani.getFrameCount(dmd)*/100000,0));
    			//project.palMappings.add(new PalMapping(-1));
    		}
        }
        
        // animationHandler.setAnimations(sourceAnis);
        if (!append) {
            animations.clear();
            selectedAnimation = null;
            selectedAnimationIndex = 0;
            playingAnis.clear();
        }
        animations.addAll(loadedList);
        aniListViewer.refresh();
    }

    private Animation buildAnimationFromFile(String filename, AnimationType type) {
        File file = new File(filename);
        String base = file.getName();
        Animation ani = new Animation(type, base, 0, 0, 1, 1, 0);
        ani.setBasePath(file.getParent() + "/");
        ani.setDesc(base.substring(0, base.indexOf('.')));
        return ani;
    }

    private Object savePalette(Event e)
    {
        FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
        fileChooser.setOverwrite(true);
        
        fileChooser.setFileName(project.palettes.get(activePaletteIndex).name);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml", "*.json" });
        fileChooser.setFilterNames(new String[] { "Paletten XML", "Paletten JSON" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename != null) {
            LOG.info("store palette to {}",filename);
            fileHelper.storeObject(project.palettes.get(activePaletteIndex), filename);
        }
        return null;
    }
    
    SmartDMDImporter smartDMDImporter = new SmartDMDImporter();
	private ComboViewer paletteComboViewer;
	private ListViewer aniListViewer;
	private ListViewer keyframeListViewer;
	private Button btnRemoveAni;
	private int selectedColor;

    private void loadPalette(Event e) {
        FileDialog fileChooser = new FileDialog(shell, SWT.OPEN);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml;*.json;*.txt" });
        fileChooser.setFilterNames(new String[] { "Palette XML", "Palette JSON", "smartdmd.txt" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename != null) {
            if( filename.toLowerCase().endsWith(".txt") ) {
                java.util.List<Palette> palettesImported = smartDMDImporter.importFromFile(filename);
                String override = checkOverride(project.palettes, palettesImported);
                if( !override.isEmpty() ) {
                    MessageBox messageBox = new MessageBox(shell,
                            SWT.ICON_WARNING | SWT.OK | SWT.IGNORE | SWT.ABORT  );
                    
                    messageBox.setText("Override warning");
                    messageBox.setMessage("importing these palettes will override palettes: "+override+
                            "\n");
                    int res = messageBox.open();
                    if( res != SWT.ABORT ) {
                        importPalettes(palettesImported,res==SWT.OK);
                    }
                } else {
                    importPalettes(palettesImported,true);
                }
            } else {
                Palette pal = (Palette) fileHelper.loadObject(filename);
                LOG.info("load palette from {}",filename);
                project.palettes.add(pal);
                activePaletteIndex = project.palettes.size()-1;
            }
            paletteComboViewer.refresh();
        }
    }
    
    private Map<Integer,Palette> getMap(java.util.List<Palette> palettes) {
        Map<Integer,Palette> res = new HashMap<>();
        for (Palette p : palettes) {
            res.put(p.index, p);
        }
        return res;
    }
    
    private void importPalettes(java.util.List<Palette> palettesImported, boolean override) {
        Map<Integer, Palette> map = getMap(project.palettes);
        for (Palette p : palettesImported) {
            if( map.containsKey(p.index) ) {
                if( override ) map.put(p.index, p);
            } else {
                map.put(p.index, p);
            }
        }
        project.palettes.clear();
        project.palettes.addAll(map.values());
    }

    private String checkOverride(java.util.List<Palette> palettes2, java.util.List<Palette> palettesImported) {
        StringBuilder sb = new StringBuilder();
        Map<Integer, Palette> map = getMap(palettes2);
        for (Palette pi : palettesImported) {
            if( pi.index != 0 && map.containsKey(pi.index)) {
                sb.append(pi.index+", ");
            }   
        }
        return sb.toString();
    }
    
    private void createColorButtons(ToolBar toolBar, int x, int y) {
        for(int i = 0; i < colBtn.length; i++) {
            colBtn[i] = new ToolItem(toolBar, SWT.RADIO);
            colBtn[i].setData(Integer.valueOf(i));
            // on mac use wider buttons e.g. 32 pix instead of 26
            //colBtn[i].setBounds(x+i*28, y, 32, 26);
            colBtn[i].setImage(getSquareImage(display, new Color(display,dmd.rgb[i])));
            colBtn[i].addListener(SWT.Selection, e -> {
                selectedColor = (Integer) e.widget.getData();
        		for (DrawTool tool : drawTools.values()) {
        			tool.setActualColor(selectedColor);
        		}
            });
        }
    }

	static Image getSquareImage(Display display, Color col) {
        Image image = new Image(display, 11, 11);
        GC gc = new GC(image);
        gc.setBackground(col);
        gc.fillRectangle(0, 0, 11, 11);
        //gc.setForeground(col);
        gc.dispose();
        return image;
      }

    private void setColorBtn() {
        for (int i = 0; i < colBtn.length; i++) {
            colBtn[i].setImage(getSquareImage(display, new Color(display,dmd.rgb[i])));
        }
    }
    
    int numberOfHashes = 4;
    Button btnHash[]  = new Button[numberOfHashes];
    
    public void createHashButtons(Composite parent, int x, int y ) {
    	for(int i = 0; i < numberOfHashes; i++) {
            btnHash[i] = new Button(parent, SWT.CHECK);
            if( i == 0 ) btnHash[i].setSelection(true);
            btnHash[i].setData(Integer.valueOf(i));
            btnHash[i].setText("Hash"+i);
            btnHash[i].setBounds(x, y+i*16, 331, 18);
            btnHash[i].addListener(SWT.Selection, e->{
            	selectedHashIndex = (Integer) e.widget.getData();
            	for(int j = 0; j < numberOfHashes; j++) {
            		if( j != selectedHashIndex ) btnHash[j].setSelection(false);
            	}
            });
    	}
    }
    
    byte[] visible = { 1,1,0,0, 1,0,0,0, 0,0,0,0, 0,0,0,1 };
	private Button btnDeleteKeyframe;
	private PalMapping selectedPalMapping;
	private long saveTimeCode;
	private Button btnAddKeyframe;
	private Button btnSetDuration;
	private Button btnPrev;
	private Button btnNext;
	private int selectedHashIndex;
	private ComboViewer paletteTypeComboViewer;
	private DMDWidget dmdWidget;
    
    private void  planesChanged(int planes, int x, int y) {
        switch(planes) {
        case 0: // 2 planes -> 4 colors
            int j = 0;
            //for(int i = 0; i < colBtn.length; i++) { colBtn[i].setLocation(x+j*28, y); if(visible[i]==1) j++; }
            for(int i = 0; i < colBtn.length; i++) colBtn[i].setEnabled(visible[i]==1); 
            break;
        case 1: // 4 planes -> 16 colors
            //for(int i = 0; i < colBtn.length; i++) colBtn[i].setLocation(x+i*28, y);
            for(int i = 0; i < colBtn.length; i++) colBtn[i].setEnabled(true);
            break;  
        }
    }


	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlPindmdEditor = new Shell();
		shlPindmdEditor.setSize(1167, 527);
		shlPindmdEditor.setText("Pin2dmd - Editor");
		shlPindmdEditor.setLayout(new GridLayout(3, false));
		
		Menu menu = new Menu(shlPindmdEditor, SWT.BAR);
		shlPindmdEditor.setMenuBar(menu);
		
		MenuItem mntmfile = new MenuItem(menu, SWT.CASCADE);
		mntmfile.setText("&File");
		
		Menu menu_1 = new Menu(mntmfile);
		mntmfile.setMenu(menu_1);
		
		MenuItem mntmNewProject = new MenuItem(menu_1, SWT.NONE);
		mntmNewProject.setText("New Project");
		
		MenuItem mntmLoadProject = new MenuItem(menu_1, SWT.NONE);
		mntmLoadProject.addListener(SWT.Selection, e-> loadProject(e));
		mntmLoadProject.setText("Load Project");
		
		MenuItem mntmSaveProject = new MenuItem(menu_1, SWT.NONE);
		mntmSaveProject.setText("Save Project");
		mntmSaveProject.addListener(SWT.Selection, e->saveProject(e));
		
		new MenuItem(menu_1, SWT.SEPARATOR);
		
		MenuItem mntmExit = new MenuItem(menu_1, SWT.NONE);
		mntmExit.setText("Exit");
		mntmExit.addListener(SWT.Selection, e->{
			// TODO addd rity check
			shlPindmdEditor.close();
            shlPindmdEditor.dispose();
		});
		
		MenuItem mntmAnimations = new MenuItem(menu, SWT.CASCADE);
		mntmAnimations.setText("&Animations");
		
		Menu menu_2 = new Menu(mntmAnimations);
		mntmAnimations.setMenu(menu_2);
		
		MenuItem mntmLoadAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmLoadAnimation.setText("Load Animation");
		mntmLoadAnimation.addListener(SWT.Selection, e->loadAniWithFC(false));
		
		MenuItem mntmAddAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmAddAnimation.setText("Add Animation");
		mntmAddAnimation.addListener(SWT.Selection, e->loadAniWithFC(true));
		
		MenuItem mntmpalettes = new MenuItem(menu, SWT.CASCADE);
		mntmpalettes.setText("&Palettes");
		
		Menu menu_3 = new Menu(mntmpalettes);
		mntmpalettes.setMenu(menu_3);
		
		MenuItem mntmLoadPalette = new MenuItem(menu_3, SWT.NONE);
		mntmLoadPalette.setText("Load Palette");
		mntmLoadPalette.addListener(SWT.Selection, e->loadPalette(e));
		
		MenuItem mntmSavePalette = new MenuItem(menu_3, SWT.NONE);
		mntmSavePalette.setText("Save Palette");
		mntmSavePalette.addListener(SWT.Selection, e->savePalette(e));
		
		mntmLoadPalette.setText("Load Palette");
		mntmLoadPalette.addListener(SWT.Selection, e->loadPalette(e));
		
		MenuItem mntmhelp = new MenuItem(menu, SWT.CASCADE);
		mntmhelp.setText("&Help");
		
		Menu menu_4 = new Menu(mntmhelp);
		mntmhelp.setMenu(menu_4);
		
		MenuItem mntmGetHelp = new MenuItem(menu_4, SWT.NONE);
		mntmGetHelp.setText("Get help");
		mntmGetHelp.addListener(SWT.Selection, e->Program.launch("http://go-dmd.de/tools/"));
		
		new MenuItem(menu_4, SWT.SEPARATOR);
		
		MenuItem mntmAbout = new MenuItem(menu_4, SWT.NONE);
		mntmAbout.setText("About");
		mntmAbout.addListener(SWT.Selection, e->new About(shell).open());
		
		Label lblAnimations = new Label(shlPindmdEditor, SWT.NONE);
		lblAnimations.setText("Animations");
		
		Label lblKeyframes = new Label(shlPindmdEditor, SWT.NONE);
		lblKeyframes.setText("KeyFrames");
		
		Label lblPreview = new Label(shlPindmdEditor, SWT.NONE);
		lblPreview.setText("Preview");
		
		aniListViewer = new ListViewer(shlPindmdEditor, SWT.BORDER | SWT.V_SCROLL);
		List aniList = aniListViewer.getList();
		GridData gd_aniList = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_aniList.widthHint = 189;
		aniList.setLayoutData(gd_aniList);
		aniListViewer.setContentProvider(ArrayContentProvider.getInstance());
		aniListViewer.setLabelProvider(new AniViewerLabelProvider());
		aniListViewer.setInput(animations);
		aniListViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (selection.size() > 0){
            	selectedAnimation = (Animation)selection.getFirstElement();
                playingAnis.clear();
                playingAnis.add(selectedAnimation);
                animationHandler.setAnimations(playingAnis); 
            } else {
            	selectedAnimation = null;
            }
            btnRemoveAni.setEnabled(selection.size()>0);
            btnAddKeyframe.setEnabled(selection.size()>0);
		});
		
		keyframeListViewer = new ListViewer(shlPindmdEditor, SWT.BORDER | SWT.V_SCROLL);
		List keyframeList = keyframeListViewer.getList();
		GridData gd_keyframeList = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_keyframeList.widthHint = 137;
		keyframeList.setLayoutData(gd_keyframeList);
		keyframeListViewer.setLabelProvider(new ViewerLabelProvider());
		keyframeListViewer.setContentProvider(ArrayContentProvider.getInstance());
		keyframeListViewer.setInput(project.palMappings);
		keyframeListViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (selection.size() > 0){
                selectedPalMapping = (PalMapping)selection.getFirstElement();
                txtDuration.setText(selectedPalMapping.durationInMillis+"");
                paletteComboViewer.setSelection(new StructuredSelection(project.palettes.get(selectedPalMapping.palIndex)));
            } else {
                selectedPalMapping = null;
            }
            btnDeleteKeyframe.setEnabled(selection.size()>0);
            btnSetDuration.setEnabled(selection.size()>0);
		});
		
		previewCanvas = new Canvas(shlPindmdEditor, SWT.BORDER|SWT.DOUBLE_BUFFERED);
        GridData gd_canvas = new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1);
        gd_canvas.heightHint = 6*32 +20;
        gd_canvas.widthHint = 128*6 + 20;
        previewCanvas.setLayoutData(gd_canvas);
        previewCanvas.setBackground(new Color(shlPindmdEditor.getDisplay(), 10,10,10));
        previewCanvas.addPaintListener(e -> { dmd.draw(e); });
        
        new Label(shlPindmdEditor, SWT.NONE);
        new Label(shlPindmdEditor, SWT.NONE);
        
        scale = new Scale(shlPindmdEditor, SWT.NONE);
        scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        scale.addListener(SWT.Selection, e -> animationHandler.setPos(scale.getSelection()));
        
        Group grpKeyframe = new Group(shlPindmdEditor, SWT.NONE);
        grpKeyframe.setLayout(new GridLayout(3, false));
        GridData gd_grpKeyframe = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 3);
        gd_grpKeyframe.widthHint = 317;
        grpKeyframe.setLayoutData(gd_grpKeyframe);
        grpKeyframe.setText("Animations / KeyFrame");
        
        Composite composite_hash = new Composite(grpKeyframe, SWT.NONE);
        GridData gd_composite_hash = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        gd_composite_hash.widthHint = 293;
        composite_hash.setLayoutData(gd_composite_hash);

        createHashButtons(composite_hash, 10, 20);
        new Label(grpKeyframe, SWT.NONE);
        
        Label lblDuration = new Label(grpKeyframe, SWT.NONE);
        lblDuration.setText("Duration:");
        
        txtDuration = new Text(grpKeyframe, SWT.BORDER);
        GridData gd_txtDuration = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_txtDuration.widthHint = 76;
        txtDuration.setLayoutData(gd_txtDuration);
        txtDuration.setText("0");
        
        btnRemoveAni = new Button(grpKeyframe, SWT.NONE);
        btnRemoveAni.setText("Remove Ani");
        btnRemoveAni.setEnabled(false);
        btnRemoveAni.addListener(SWT.Selection, e->{
            if( selectedAnimation != null ) {
                animations.remove(selectedAnimation);
                aniListViewer.refresh();
                playingAnis.clear();
                animationHandler.setAnimations(playingAnis);
                animationHandler.setClockActive(true);
            }
        });
        new Label(grpKeyframe, SWT.NONE);
        
        btnAddKeyframe = new Button(grpKeyframe, SWT.NONE);
        btnAddKeyframe.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnAddKeyframe.setText("Add KeyFrame");
        btnAddKeyframe.setEnabled(false);
        btnAddKeyframe.addListener(SWT.Selection, e->{
        	PalMapping palMapping = new PalMapping(activePaletteIndex);
        	if( selectedHashIndex == -1 ) {
        		
        	} else {
        		palMapping.setDigest(hashes.get(selectedHashIndex));
        	}
        	palMapping.name = "KeyFrame "+project.palMappings.size()+1;
        	project.palMappings.add(palMapping);
        	saveTimeCode = lastTimeCode;
        	keyframeListViewer.refresh();
        });
        new Label(grpKeyframe, SWT.NONE);
        new Label(grpKeyframe, SWT.NONE);
        
        btnSetDuration = new Button(grpKeyframe, SWT.NONE);
        btnSetDuration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnSetDuration.setText("Set Duration");
        btnSetDuration.setEnabled(false);
        btnSetDuration.addListener(SWT.Selection, e->{
        	if( selectedPalMapping!=null) {
        		selectedPalMapping.durationInMillis = lastTimeCode -saveTimeCode;
        		txtDuration.setText(selectedPalMapping.durationInMillis+"");
        	}
        });
        new Label(grpKeyframe, SWT.NONE);
        new Label(grpKeyframe, SWT.NONE);
        
        btnDeleteKeyframe = new Button(grpKeyframe, SWT.NONE);
        btnDeleteKeyframe.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnDeleteKeyframe.setText("Del KeyFrame");
        btnDeleteKeyframe.setEnabled(false);
        btnDeleteKeyframe.addListener(SWT.Selection, e->{
        	if( selectedPalMapping!=null) {
        		project.palMappings.remove(selectedPalMapping);
        		keyframeListViewer.refresh();
        	}
        });
        
        Group grpDetails = new Group(shlPindmdEditor, SWT.NONE);
        grpDetails.setLayout(new GridLayout(4, false));
        GridData gd_grpDetails = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd_grpDetails.heightHint = 21;
        gd_grpDetails.widthHint = 231;
        grpDetails.setLayoutData(gd_grpDetails);
        grpDetails.setText("Details");
        
        Label lblFrame = new Label(grpDetails, SWT.NONE);
        lblFrame.setText("Frame:");
        
        lblFrameNo = new Label(grpDetails, SWT.NONE);
        GridData gd_lblFrameNo = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblFrameNo.minimumWidth = 60;
        lblFrameNo.setLayoutData(gd_lblFrameNo);
        lblFrameNo.setText("xxxxxxxx");
        
        Label lblTimecode = new Label(grpDetails, SWT.NONE);
        lblTimecode.setText("Timecode:");
        
        lblTcval = new Label(grpDetails, SWT.NONE);
        GridData gd_lblTcval = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblTcval.minimumWidth = 80;
        lblTcval.setLayoutData(gd_lblTcval);
        lblTcval.setText("xxxxxxxxxx");
        
        Composite composite = new Composite(shlPindmdEditor, SWT.NONE);
        composite.setLayout(new GridLayout(4, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        
        Button btnStart = new Button(composite, SWT.NONE);
        Button btnStop = new Button(composite, SWT.NONE);
        btnStart.setText("Start");
        btnStart.addListener(SWT.Selection, e-> {
        	animationHandler.start();
    		btnStop.setEnabled(true);
    		btnStart.setEnabled(false);
        	btnPrev.setEnabled(false);
        	btnNext.setEnabled(false);
        });
        btnStart.setEnabled(false);
        btnStop.setText("Stop");
        btnStop.addListener(SWT.Selection, e->{
        	animationHandler.stop();
        	btnStop.setEnabled(false);
        	btnStart.setEnabled(true);
        	btnPrev.setEnabled(true);
        	btnNext.setEnabled(true);
        });
        
        btnPrev = new Button(composite, SWT.NONE);
        btnPrev.setText("<");
        btnPrev.setEnabled(false);
        btnPrev.addListener(SWT.Selection, e->animationHandler.prev());
        
        btnNext = new Button(composite, SWT.NONE);
        btnNext.setText(">");
        btnNext.setEnabled(false);
        btnNext.addListener(SWT.Selection, e->animationHandler.next());
        
        Group grpPalettes = new Group(shlPindmdEditor, SWT.NONE);
        grpPalettes.setLayout(new GridLayout(8, false));
        GridData gd_grpPalettes = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd_grpPalettes.heightHint = 90;
        grpPalettes.setLayoutData(gd_grpPalettes);
        grpPalettes.setText("Palettes");

        paletteComboViewer = new ComboViewer(grpPalettes, SWT.NONE);
        paletteComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        paletteComboViewer.setLabelProvider(new PaletteViewerLabelProvider());
        paletteComboViewer.setInput(project.palettes);
        paletteComboViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (selection.size() > 0) {
                Palette pal = (Palette) selection.getFirstElement();
                activePaletteIndex = pal.index;
                dmd.rgb = pal.colors;
                dmdWidget.setPalette(pal);
                setColorBtn();
                paletteTypeComboViewer.setSelection(new StructuredSelection(pal.type));
            }
        });

        paletteTypeComboViewer = new ComboViewer(grpPalettes, SWT.READ_ONLY);
        paletteTypeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        paletteTypeComboViewer.setInput(PaletteType.values());
        paletteTypeComboViewer.setSelection(new StructuredSelection(PaletteType.NORMAL));
        paletteTypeComboViewer.addSelectionChangedListener(e -> {
            IStructuredSelection selection = (IStructuredSelection) e.getSelection();
            PaletteType palType = (PaletteType) selection.getFirstElement();
            project.palettes.get(activePaletteIndex).type = palType;
            if (!PaletteType.NORMAL.equals(palType)) {
                for (int i = 0; i < project.palettes.size(); i++) {
                    if (i != activePaletteIndex) { // set all other to normal
                        project.palettes.get(activePaletteIndex).type = PaletteType.NORMAL;
                    }
                }
            }
        })  ;

        Button btnNew = new Button(grpPalettes, SWT.NONE);
        btnNew.setText("New");
        btnNew.addListener(SWT.Selection, e -> {
            String name = this.paletteComboViewer.getCombo().getText();
            if (!isNewPaletteName(name)) {
                name = "new" + UUID.randomUUID().toString().substring(0, 4);
            }
            Palette p = new Palette(dmd.rgb, project.palettes.size(), name);
            project.palettes.add(p);
            paletteComboViewer.refresh();
            activePaletteIndex = project.palettes.size() - 1;
            paletteComboViewer.getCombo().select(activePaletteIndex);
        });

        Button btnRename = new Button(grpPalettes, SWT.NONE);
        btnRename.setText("Rename");
        btnRename.addListener(SWT.Selection, e -> {
            project.palettes.get(activePaletteIndex).name = paletteComboViewer.getCombo().getText().split(" - ")[1];
            paletteComboViewer.getCombo().select(activePaletteIndex);
            paletteComboViewer.refresh();
        });

        Button btnReset = new Button(grpPalettes, SWT.NONE);
        btnReset.setText("Reset");
        btnReset.addListener(SWT.Selection, e -> {
            dmd.resetColors();
            setColorBtn();
        });

        Label lblPlanes = new Label(grpPalettes, SWT.NONE);
        lblPlanes.setText("Planes");

        ComboViewer planesComboViewer = new ComboViewer(grpPalettes, SWT.READ_ONLY);
        Combo planes = planesComboViewer.getCombo();
        planes.setItems(new String[] { "2", "4" });
        planes.setToolTipText("Number of planes");
        planes.select(1);
        planes.addListener(SWT.Selection, e -> planesChanged(planes.getSelectionIndex(), 10, 10));
        
        Button btnUploadPalette = new Button(grpPalettes, SWT.NONE);
        btnUploadPalette.setText("Upload Palette");

        ToolBar paletteBar = new ToolBar(grpPalettes, SWT.FLAT | SWT.RIGHT);
        GridData gd_composite_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1);
        gd_composite_1.widthHint = 420;
        paletteBar.setLayoutData(gd_composite_1);
        
        createColorButtons(paletteBar,20,10);

        planesChanged(0,10,10);
        
        drawTools.put("pencil", new SetPixelTool(selectedColor));       
        drawTools.put("fill", new FloodFillTool(selectedColor));       
        drawTools.put("rect", new RectTool(selectedColor));       
        
        Button btnColor = new Button(grpPalettes, SWT.NONE);
        btnColor.setText("Color");
        btnColor.addListener(SWT.Selection, e->{
          ColorDialog cd = new ColorDialog(shell);
          cd.setText("Select new color");
          cd.setRGB(dmd.getColor(selectedColor));
          RGB rgb = cd.open();
          if (rgb == null) {
              return;
          }
          colBtn[selectedColor].setImage(getSquareImage(display, new Color(display,rgb)));
          dmd.setColor(selectedColor, rgb);
          Palette pal = project.palettes.get(activePaletteIndex);
          pal.colors[selectedColor] = rgb;
          dmdWidget.setPalette(pal);

        });
        
        ToolBar toolBar = new ToolBar(grpPalettes, SWT.FLAT | SWT.RIGHT);
        toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
        
        ToolItem tltmPen = new ToolItem(toolBar, SWT.RADIO);
        tltmPen.setImage(SWTResourceManager.getImage(PinDmdEditor.class, "/icons/pencil.png"));
        tltmPen.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("pencil")));
        
        ToolItem tltmFill = new ToolItem(toolBar, SWT.RADIO);
        tltmFill.setImage(SWTResourceManager.getImage(PinDmdEditor.class, "/icons/color-fill.png"));
        tltmFill.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("fill")));
        
        ToolItem tltmRect = new ToolItem(toolBar, SWT.RADIO);
        tltmRect.setImage(SWTResourceManager.getImage(PinDmdEditor.class, "/icons/rect.png"));
        tltmRect.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("rect")));
        
                ToolItem tltmEraser = new ToolItem(toolBar, SWT.RADIO);
                tltmEraser.setImage(SWTResourceManager.getImage(PinDmdEditor.class, "/icons/eraser.png"));
                tltmEraser.addListener(SWT.Selection, e->dmdWidget.setDrawTool(null));
        new Label(grpPalettes, SWT.NONE);
        new Label(grpPalettes, SWT.NONE);

        dmdWidget = new DMDWidget(shlPindmdEditor, SWT.DOUBLE_BUFFERED, this.dmd);
        dmdWidget.setBounds(0, 0, 600, 200);
        GridData gd_dmdWidget = new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1);
        gd_dmdWidget.heightHint = 200;
        dmdWidget.setLayoutData(gd_dmdWidget);
        
    }
	

	public String getPrintableHashes(byte[] p) {
		StringBuffer hexString = new StringBuffer();
		for (int j = 0; j < p.length; j++)
			hexString.append(String.format("%02X ", p[j]));
		return hexString.toString();
	}
	
    private boolean isNewPaletteName(String text) {
        for(Palette pal : project.palettes) {
            if( pal.name.equals(text)) return false;
        }
        return true;
    }
}
