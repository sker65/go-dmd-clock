package com.rinke.solutions.pinball;

import java.io.File;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.layout.GridData;
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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Scene;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PinDmdEditor {

    private class DmdPaintListener implements PaintListener {

        public void paintControl(PaintEvent e) {
            dmd.draw(e);
            if (animationHandler != null)
                e.display.timerExec(animationHandler.getRefreshDelay(), animationHandler);
            e.gc.dispose();
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
	
	Project project = new Project();
	java.util.List<Animation> animations = new ArrayList<>();
	
	protected Shell shlPindmdEditor;
	private Text txtDuration;
	protected long lastTimeCode;
    Display display;
    final Button colBtn[] = new Button[16];
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
		
		project.palettes.add(new Palette(dmd.rgb, 0, "default"));

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
                    String[] hashes = evt.getPrintableHashes().replaceAll("plane 1","#plane 1").split("#");
                    btnHash1.setText(hashes[0]);
                    btnHash2.setText(hashes[1]);
                    btnHash3.setText("");

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
        

        shell = shlPindmdEditor.getShell();
		shlPindmdEditor.open();
		shlPindmdEditor.layout();
		

		GlobalExceptionHandler.getInstance().setDisplay(display);
        GlobalExceptionHandler.getInstance().setShell(shell);

		display.timerExec(animationHandler.getRefreshDelay(), animationHandler);
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
                if( retry++ > 10 ) System.exit(1);
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
	private int activePalette;
	
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
    			project.palMappings.add(new PalMapping(-1));
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
        
        fileChooser.setFileName(project.palettes.get(activePalette).name);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml", "*.json" });
        fileChooser.setFilterNames(new String[] { "Paletten XML", "Paletten JSON" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename != null) {
            LOG.info("store palette to {}",filename);
            fileHelper.storeObject(project.palettes.get(activePalette), filename);
        }
        return null;
    }
    
    SmartDMDImporter smartDMDImporter = new SmartDMDImporter();
	private ComboViewer paletteComboViewer;
	private ListViewer aniListViewer;
	private ListViewer keyframeListViewer;
	private Button btnRemoveAni;

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
                activePalette = project.palettes.size()-1;
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
    
    private void createColorButtons(Group grp, int x, int y) {
        for(int i = 0; i < colBtn.length; i++) {
            colBtn[i] = new Button(grp, SWT.FLAT+SWT.TOGGLE);
            colBtn[i].setData(Integer.valueOf(i));
            // on mac use wider buttons e.g. 32 pix instead of 26
            colBtn[i].setBounds(x+i*28, y, 32, 26);
            colBtn[i].setImage(getSquareImage(display, new Color(display,dmd.rgb[i])));
            colBtn[i].addListener(SWT.Selection, e -> {
                ColorDialog cd = new ColorDialog(shell);
                cd.setText("ColorDialog Demo");
                int j = (Integer) e.widget.getData();
                cd.setRGB(dmd.getColor(j));
                RGB newColor = cd.open();
                if (newColor == null) {
                    return;
                }
                ((Button)e.widget).setImage(getSquareImage(display, new Color(display,newColor)));
                dmd.setColor(j, newColor);
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
    
    byte[] visible = { 1,1,0,0, 0,0,0,1, 0,0,0,0, 0,0,0,1 };
	private Button btnHash1;
	private Button btnHash2;
	private Button btnHash3;
	private Button btnDeleteKeyframe;
	private PalMapping selectedPalMapping;
	private long saveTimeCode;
	private Button btnAddKeyframe;
	private Button btnSetDuration;
    
    private void  planesChanged(int planes, int x, int y) {
        switch(planes) {
        case 0: // 2 planes -> 4 colors
            int j = 0;
            for(int i = 0; i < colBtn.length; i++) { colBtn[i].setLocation(x+j*28, y); if(visible[i]==1) j++; }
            for(int i = 0; i < colBtn.length; i++) colBtn[i].setVisible(visible[i]==1); 
            break;
        case 1: // 4 planes -> 16 colors
            for(int i = 0; i < colBtn.length; i++) colBtn[i].setLocation(x+i*28, y);
            for(int i = 0; i < colBtn.length; i++) colBtn[i].setVisible(true);
            break;  
        }
    }


	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlPindmdEditor = new Shell();
		shlPindmdEditor.setSize(1267, 575);
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
                Animation ani = (Animation)selection.getFirstElement();
                playingAnis.clear();
                playingAnis.add(ani);
                animationHandler.setAnimations(playingAnis);
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
        gd_canvas.heightHint = 250;
        gd_canvas.widthHint = 960;
        previewCanvas.setLayoutData(gd_canvas);
        previewCanvas.setBackground(new Color(shlPindmdEditor.getDisplay(), 10,10,10));
        previewCanvas.addPaintListener(new DmdPaintListener());
        new Label(shlPindmdEditor, SWT.NONE);
        new Label(shlPindmdEditor, SWT.NONE);
        
        scale = new Scale(shlPindmdEditor, SWT.NONE);
        scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        scale.addListener(SWT.Selection, e -> animationHandler.setPos(scale.getSelection()));
        
        Group grpKeyframe = new Group(shlPindmdEditor, SWT.NONE);
        GridData gd_grpKeyframe = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 3);
        gd_grpKeyframe.widthHint = 255;
        grpKeyframe.setLayoutData(gd_grpKeyframe);
        grpKeyframe.setText("Animations / KeyFrame");
        
        btnHash1 = new Button(grpKeyframe, SWT.CHECK);
        btnHash1.setText("Hash1");
        btnHash1.setBounds(20, 10, 331, 18);
        
        btnHash2 = new Button(grpKeyframe, SWT.CHECK);
        btnHash2.setBounds(20, 26, 331, 18);
        btnHash2.setText("Hash2");
        
        btnHash3 = new Button(grpKeyframe, SWT.CHECK);
        btnHash3.setBounds(20, 42, 331, 18);
        btnHash3.setText("Hash3");
        
        txtDuration = new Text(grpKeyframe, SWT.BORDER);
        txtDuration.setText("0");
        txtDuration.setBounds(260, 71, 64, 19);
        
        Label lblDuration = new Label(grpKeyframe, SWT.NONE);
        lblDuration.setBounds(205, 74, 53, 16);
        lblDuration.setText("Duration:");
        
        btnRemoveAni = new Button(grpKeyframe, SWT.NONE);
        btnRemoveAni.setBounds(10, 123, 94, 28);
        btnRemoveAni.setText("Remove Ani");
        btnRemoveAni.setEnabled(false);
        
        btnDeleteKeyframe = new Button(grpKeyframe, SWT.NONE);
        btnDeleteKeyframe.setBounds(205, 123, 119, 28);
        btnDeleteKeyframe.setText("Del KeyFrame");
        btnDeleteKeyframe.setEnabled(false);
        btnDeleteKeyframe.addListener(SWT.Selection, e->{
        	if( selectedPalMapping!=null) {
        		project.palMappings.remove(selectedPalMapping);
        		keyframeListViewer.refresh();
        	}
        });
        
        btnAddKeyframe = new Button(grpKeyframe, SWT.NONE);
        btnAddKeyframe.setText("Add KeyFrame");
        btnAddKeyframe.setBounds(205, 96, 119, 28);
        btnAddKeyframe.setEnabled(false);
        btnAddKeyframe.addListener(SWT.Selection, e->{
        	PalMapping palMapping = new PalMapping(activePalette);
        	if( btnHash1.getSelection() ) palMapping.setDigest(hashes.get(0));
        	if( btnHash1.getSelection() ) palMapping.setDigest(hashes.get(1));
        	palMapping.name = "KeyFrame "+project.palMappings.size()+1;
        	project.palMappings.add(palMapping);
        	saveTimeCode = lastTimeCode;
        	keyframeListViewer.refresh();
        });
        
        btnSetDuration = new Button(grpKeyframe, SWT.NONE);
        btnSetDuration.setBounds(205, 146, 119, 28);
        btnSetDuration.setText("Set Duration");
        btnSetDuration.setEnabled(false);
        btnSetDuration.addListener(SWT.Selection, e->{
        	if( selectedPalMapping!=null) {
        		selectedPalMapping.durationInMillis = lastTimeCode -saveTimeCode;
        		txtDuration.setText(selectedPalMapping.durationInMillis+"");
        	}
        });
        
        Group grpDetails = new Group(shlPindmdEditor, SWT.NONE);
        GridData gd_grpDetails = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd_grpDetails.widthHint = 231;
        grpDetails.setLayoutData(gd_grpDetails);
        grpDetails.setText("Details");
        
        Label lblFrame = new Label(grpDetails, SWT.NONE);
        lblFrame.setBounds(10, 10, 48, 14);
        lblFrame.setText("Frame:");
        
        lblFrameNo = new Label(grpDetails, SWT.NONE);
        lblFrameNo.setBounds(59, 10, 59, 14);
        lblFrameNo.setText("");
        
        Label lblTimecode = new Label(grpDetails, SWT.NONE);
        lblTimecode.setBounds(152, 10, 59, 14);
        lblTimecode.setText("Timecode:");
        
        lblTcval = new Label(grpDetails, SWT.NONE);
        lblTcval.setBounds(225, 10, 59, 14);
        lblTcval.setText("");
        
        Composite composite = new Composite(shlPindmdEditor, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        
        Button btnStart = new Button(composite, SWT.NONE);
        Button btnStop = new Button(composite, SWT.NONE);

        btnStart.setBounds(0, 0, 94, 28);
        btnStart.setText("Start");
        btnStart.addListener(SWT.Selection, e-> {
        	animationHandler.start();
    		btnStop.setEnabled(true);
    		btnStart.setEnabled(false);
        });
        btnStart.setEnabled(false);
        
        btnStop.setBounds(100, 0, 94, 28);
        btnStop.setText("Stop");
        btnStop.addListener(SWT.Selection, e->{
        	animationHandler.stop();
        	btnStop.setEnabled(false);
        	btnStart.setEnabled(true);
        });
        
        Button btnPrev = new Button(composite, SWT.NONE);
        btnPrev.setBounds(200, 0, 40, 28);
        btnPrev.setText("<");
        btnPrev.addListener(SWT.Selection, e->animationHandler.prev());
        
        Button btnNext = new Button(composite, SWT.NONE);
        btnNext.setText(">");
        btnNext.setBounds(236, 0, 40, 28);
        btnNext.addListener(SWT.Selection, e->animationHandler.next());
        
        Group grpPalettes = new Group(shlPindmdEditor, SWT.NONE);
        GridData gd_grpPalettes = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd_grpPalettes.heightHint = 90;
        grpPalettes.setLayoutData(gd_grpPalettes);
        grpPalettes.setText("Palettes");
        createColorButtons(grpPalettes,5,40);
        
        Button btnDefault = new Button(grpPalettes, SWT.CHECK);

        paletteComboViewer = new ComboViewer(grpPalettes, SWT.NONE);
        Combo palettes = paletteComboViewer.getCombo();
        palettes.setBounds(10, 10, 122, 22);
        paletteComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        paletteComboViewer.setLabelProvider(new PaletteViewerLabelProvider());
        paletteComboViewer.setInput(project.palettes);
        paletteComboViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                  if (selection.size() > 0){
                      Palette pal = (Palette)selection.getFirstElement();
                      activePalette = pal.index;
                      dmd.rgb = pal.colors;
                      setColorBtn();
                      btnDefault.setSelection( pal.isDefault ); 
                  }
        });
        
        btnDefault.setBounds(138, 10, 67, 18);
        btnDefault.setText("default");
        btnDefault.addListener(SWT.Selection, e -> {
            boolean isDefault = btnDefault.getSelection();
            if( isDefault ) {
                // clean default state from others
                project.palettes.stream().forEach(p->p.isDefault=false);
            }
            project.palettes.get(activePalette).isDefault = isDefault;
        });

        Button btnNew = new Button(grpPalettes, SWT.NONE);
        btnNew.setBounds(198, 4, 67, 28);
        btnNew.setText("New");
        btnNew.addListener(SWT.Selection, e -> {
            String name = this.paletteComboViewer.getCombo().getText();
            if( !isNewPaletteName(name)) {
                name = "new"+UUID.randomUUID().toString().substring(0, 4);
            }
            Palette p = new Palette(dmd.rgb,project.palettes.size(), name);
            project.palettes.add(p);
            paletteComboViewer.refresh();
            activePalette = project.palettes.size()-1;
        });
        
        Button btnRename = new Button(grpPalettes, SWT.NONE);
        btnRename.setBounds(260, 4, 75, 28);
        btnRename.setText("Rename");
        btnRename.addListener(SWT.Selection, e -> {
            project.palettes.get(activePalette).name = paletteComboViewer.getCombo().getText().split(" - ")[1];
            paletteComboViewer.getCombo().select(activePalette);
            paletteComboViewer.refresh();
        });
        
        
        Button btnReset = new Button(grpPalettes, SWT.NONE);
        btnReset.setBounds(333, 4, 67, 28);
        btnReset.setText("Reset");
        btnReset.addListener(SWT.Selection, e -> { 
            dmd.resetColors();
            setColorBtn();
        });
 
        ComboViewer planesComboViewer = new ComboViewer(grpPalettes, SWT.READ_ONLY);
        Combo planes = planesComboViewer.getCombo();
        planes.setItems(new String[]{"2","4"});
        planes.setToolTipText("Number of planes");
        planes.setBounds(465, 6, 46, 22);
        planes.select(0);
        planes.select(0);
        planesChanged(0,5,40);
        planes.addListener(SWT.Selection, e -> planesChanged( planes.getSelectionIndex(),5,40 ));

        
        Label lblPlanes = new Label(grpPalettes, SWT.NONE);
        lblPlanes.setBounds(400, 10, 53, 14);
        lblPlanes.setText("planes");

	}
	
    private boolean isNewPaletteName(String text) {
        for(Palette pal : project.palettes) {
            if( pal.name.equals(text)) return false;
        }
        return true;
    }

}
