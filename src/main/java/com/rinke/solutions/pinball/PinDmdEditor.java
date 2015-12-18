package com.rinke.solutions.pinball;

import java.awt.SplashScreen;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationCompiler;
import com.rinke.solutions.pinball.animation.AnimationFactory;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.io.SmartDMDImporter;
import com.rinke.solutions.pinball.io.UsbTool;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.model.Scene;
import com.rinke.solutions.pinball.ui.About;
import com.rinke.solutions.pinball.ui.FileChooser;
import com.rinke.solutions.pinball.ui.FileDialogDelegate;
import com.rinke.solutions.pinball.widget.CircleTool;
import com.rinke.solutions.pinball.widget.DMDWidget;
import com.rinke.solutions.pinball.widget.DrawTool;
import com.rinke.solutions.pinball.widget.FloodFillTool;
import com.rinke.solutions.pinball.widget.LineTool;
import com.rinke.solutions.pinball.widget.PaletteTool;
import com.rinke.solutions.pinball.widget.RectTool;
import com.rinke.solutions.pinball.widget.SetPixelTool;


public class PinDmdEditor {

	private static final Logger LOG = LoggerFactory.getLogger(PinDmdEditor.class);
	
	private static final int FRAME_RATE = 40;

	private static final String HELP_URL = "http://go-dmd.de/2015/11/24/pin2dmd-editor/";

	DMD dmd = new DMD(128,32);
	AnimationHandler animationHandler = null;
	
	CyclicRedraw cyclicRedraw = new CyclicRedraw();
	java.util.List<Animation> animations = new ArrayList<>();
    Map<String,DrawTool> drawTools = new HashMap<>();

    Display display;
	protected Shell shell;

	protected long lastTimeCode;

    //private Canvas previewCanvas;
	private Label lblTcval;
	private Label lblFrameNo;
	private Scale scale;
	
	private String lastPath;
	private String frameTextPrefix = "Pin2dmd Editor ";
	private Animation selectedAnimation = null;
	private int selectedAnimationIndex = 0;
	private java.util.List<Animation> playingAnis = new ArrayList<Animation>();
	Palette activePalette;

	// colaboration classes
    private DMDClock clock = new DMDClock(false);
	FileHelper fileHelper = new FileHelper();
    SmartDMDImporter smartDMDImporter = new SmartDMDImporter();
    UsbTool usbTool = new UsbTool();
    
    Project project = new Project();

    int numberOfHashes = 4;
    java.util.List<byte[]> hashes = new ArrayList<byte[]>();
    
	/** instance level SWT widgets */
	Button btnHash[] = new Button[numberOfHashes];
	Text txtDuration;
	ComboViewer paletteComboViewer;
	ListViewer aniListViewer;
	ListViewer keyframeListViewer;
	Button btnRemoveAni;
	Button btnDeleteKeyframe;
	Button btnAddKeyframe;
	Button btnFetchDuration;
	Button btnPrev;
	Button btnNext;
	ComboViewer paletteTypeComboViewer;
	DMDWidget dmdWidget;
	ResourceManager resManager;

	Button btnChangeColor;
	Button btnNewPalette;
	Button btnRenamePalette;

	PaletteTool paletteTool;
	int selectedHashIndex;
	PalMapping selectedPalMapping;
	long saveTimeCode;
    int cutStart;
    int cutEnd;

	private int[] numberOfPlanes = { 2 , 4 };

	private int actualNumberOfPlanes = 4;
    
	public PinDmdEditor() {
		super();
	    activePalette = project.palettes.get(0);
	}

	/**
	 * handles redraw of animations
	 * @author steve
	 */
    private class CyclicRedraw implements Runnable {

		@Override
		public void run() {
			//if( !previewCanvas.isDisposed()) previewCanvas.redraw();
			if( dmdWidget!=null && !dmdWidget.isDisposed() ) dmdWidget.redraw();
            if (animationHandler != null && !animationHandler.isStopped()) {
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
		shell = new Shell();
		
		createContents(shell);

		createNewProject();
		
		paletteComboViewer.getCombo().select(0);
		paletteTool.setPalette(activePalette);
		
		animationHandler = new AnimationHandler(playingAnis, clock, dmd, dmdWidget, false);
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

		shell.open();
		shell.layout();
		shell.addListener(SWT.Close,  e -> {
			e.doit = dirtyCheck();
		});

		GlobalExceptionHandler.getInstance().setDisplay(display);
        GlobalExceptionHandler.getInstance().setShell(shell);

		display.timerExec(animationHandler.getRefreshDelay(), cyclicRedraw);
		
		loadAni("./src/test/resources/drwho-dump.txt.gz", false, true);
		Animation cutScene = animations.get(0).cutScene( 0, 200, actualNumberOfPlanes);
		cutScene.setDesc("foo");
		animations.add(cutScene);
		aniListViewer.refresh();
		aniListViewer.setSelection(new StructuredSelection(cutScene));
		
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
	
    void createNewProject() {
	    project = new Project();
	    activePalette = project.palettes.get(0);
    	paletteComboViewer.setInput(project.palettes);
    	keyframeListViewer.setInput(project.palMappings);
	    animations.clear();
	    aniListViewer.refresh();
	    playingAnis.clear();
	    selectedAnimation = null;
	}

	private void loadProject() {
		FileChooser fileChooser = createFileChooser(shell, SWT.OPEN);
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
            	shell.setText(frameTextPrefix+" - "+new File(filename).getName());
                project = projectToLoad;
                
                if( project.inputFiles.size() >0 ) loadAni(project.inputFiles.get(0), false, false);
                for( int i = 1; i < project.scenes.size(); i++) {
                	//cutOutNewAnimation(project.scenes.get(i).start, project.scenes.get(i).end, animations.get(0));
                	LOG.info("cutting out "+project.scenes.get(i));
                }
                aniListViewer.refresh();
                paletteComboViewer.setInput(project.palettes);
                keyframeListViewer.setInput(project.palMappings);
            }
        }
        
        // TODO recreate animation list on load of project
        // which means reload initial source file and recreate
        // scenes and populate source list

    }

    private void saveProject() {
    	FileChooser fileChooser = createFileChooser(shell, SWT.SAVE);
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
        project.dirty = false;
    }

    protected void loadAniWithFC(boolean append) {
        FileChooser fileChooser = createFileChooser(shell, SWT.OPEN);
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
    
    public void loadAni(String filename, boolean append, boolean populateProject) {
        java.util.List<Animation> loadedList = new ArrayList<>();
        if (filename.endsWith(".ani")) {
            loadedList.addAll(AnimationCompiler.readFromCompiledFile(filename));
        } else if (filename.endsWith(".txt.gz")) {
            loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.MAME));
        } else if (filename.endsWith(".properties")) {
            loadedList.addAll(AnimationFactory.createAnimationsFromProperties(filename));
        } else if (filename.endsWith(".pcap") || filename.endsWith(".pcap.gz") ) {
        	loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.PCAP));
        }
        
        if( populateProject ) {
            if( !append ) project.inputFiles.clear();
            project.inputFiles.add(filename);
            //DMD dmd = new DMD(128, 32);
//            for (Animation ani : loadedList) {
//    			project.scenes.add(new Scene(ani.getDesc(),0,/*ani.getFrameCount(dmd)*/100000,0));
//    			//project.palMappings.add(new PalMapping(-1));
//    		}
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
        project.dirty = true;
    }

    private void savePalette()
    {
        FileChooser fileChooser = createFileChooser(shell, SWT.SAVE);
        fileChooser.setOverwrite(true);
        fileChooser.setFileName(activePalette.name);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml", "*.json" });
        fileChooser.setFilterNames(new String[] { "Paletten XML", "Paletten JSON" });
        String filename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
        if (filename != null) {
            LOG.info("store palette to {}",filename);
            fileHelper.storeObject(activePalette, filename);
        }
    }
    
    private void loadPalette() {
        FileChooser fileChooser = createFileChooser(shell, SWT.OPEN);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(new String[] { "*.xml","*.json,", "*.txt" });
        fileChooser.setFilterNames(new String[] { "Palette XML", "Palette JSON", "smartdmd" });
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
                activePalette = pal;
            }
            paletteComboViewer.setSelection(new StructuredSelection(activePalette));
            paletteComboViewer.refresh();
        }
    }
    
    // testability overridden by tests
    protected FileChooser createFileChooser(Shell shell, int flags) {	
		return new FileDialogDelegate(shell, flags);
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

    public void createHashButtons(Composite parent, int x, int y ) {
    	for(int i = 0; i < numberOfHashes; i++) {
            btnHash[i] = new Button(parent, SWT.CHECK);
            if( i == 0 ) btnHash[i].setSelection(true);
            btnHash[i].setData(Integer.valueOf(i));
            btnHash[i].setText("Hash"+i);
            //btnHash[i].setFont(new Font(shell.getDisplay(), "sans", 10, 0));
            btnHash[i].setBounds(x, y+i*16, 331, 18);
            btnHash[i].addListener(SWT.Selection, e->{
            	selectedHashIndex = (Integer) e.widget.getData();
            	if( selectedPalMapping != null ) {
            	    selectedPalMapping.hashIndex = selectedHashIndex;
            	}
            	for(int j = 0; j < numberOfHashes; j++) {
            		if( j != selectedHashIndex ) btnHash[j].setSelection(false);
            	}
            });
    	}
    }
    

	/**
	 * Create contents of the window.
	 */
	 void createContents(Shell shell) {
		shell.setSize(1167, 553);
		shell.setText("Pin2dmd - Editor");
		shell.setLayout(new GridLayout(3, false));
		
		createMenu(shell);

		resManager = new LocalResourceManager(JFaceResources.getResources(),shell);
		
		Label lblAnimations = new Label(shell, SWT.NONE);
		lblAnimations.setText("Animations");
		
		Label lblKeyframes = new Label(shell, SWT.NONE);
		lblKeyframes.setText("KeyFrames");
		
		Label lblPreview = new Label(shell, SWT.NONE);
		lblPreview.setText("Preview");
		
		aniListViewer = new ListViewer(shell, SWT.BORDER | SWT.V_SCROLL);
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
            	selectedAnimationIndex = aniList.getSelectionIndex();
                playingAnis.clear();
                playingAnis.add(selectedAnimation);
                animationHandler.setAnimations(playingAnis);
                dmdWidget.redraw();
            } else {
            	selectedAnimation = null;
            }
            btnRemoveAni.setEnabled(selection.size()>0);
            btnAddKeyframe.setEnabled(selection.size()>0);
		});
		
		keyframeListViewer = new ListViewer(shell, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		List keyframeList = keyframeListViewer.getList();
		GridData gd_keyframeList = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_keyframeList.widthHint = 137;
		keyframeList.setLayoutData(gd_keyframeList);
		keyframeListViewer.setLabelProvider(new ViewerLabelProvider());
		keyframeListViewer.setContentProvider(ArrayContentProvider.getInstance());
		keyframeListViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (selection.size() > 0) {
            	// set new mapping
                selectedPalMapping = (PalMapping)selection.getFirstElement();
                selectedHashIndex = selectedPalMapping.hashIndex;
                
                txtDuration.setText(selectedPalMapping.durationInMillis+"");
                paletteComboViewer.setSelection(new StructuredSelection(project.palettes.get(selectedPalMapping.palIndex)));
                for(int j = 0; j < numberOfHashes; j++) {
                    btnHash[j].setSelection(j == selectedHashIndex);
                }
                selectedAnimationIndex = selectedPalMapping.animationIndex;
                selectedAnimation = animations.get(selectedAnimationIndex);
                aniListViewer.setSelection(new StructuredSelection(selectedAnimation));
                
                animationHandler.setPos(selectedPalMapping.frameIndex);
                saveTimeCode = selectedAnimation.getTimeCode(selectedPalMapping.frameIndex);
            } else {
                selectedPalMapping = null;
            }
            btnDeleteKeyframe.setEnabled(selection.size()>0);
            btnFetchDuration.setEnabled(selection.size()>0);
		});
		
        dmdWidget = new DMDWidget(shell, SWT.DOUBLE_BUFFERED, this.dmd);
        dmdWidget.setBounds(0, 0, 600, 200);
        GridData gd_dmdWidget = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd_dmdWidget.heightHint = 200;
        dmdWidget.setLayoutData(gd_dmdWidget);
        
        new Label(shell, SWT.NONE);
        new Label(shell, SWT.NONE);
        
        scale = new Scale(shell, SWT.NONE);
        scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        scale.addListener(SWT.Selection, e -> animationHandler.setPos(scale.getSelection()));
        
        Group grpKeyframe = new Group(shell, SWT.NONE);
        grpKeyframe.setLayout(new GridLayout(3, false));
        GridData gd_grpKeyframe = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 3);
        gd_grpKeyframe.widthHint = 350;
        grpKeyframe.setLayoutData(gd_grpKeyframe);
        grpKeyframe.setText("Animations / KeyFrame");
        
        Composite composite_hash = new Composite(grpKeyframe, SWT.NONE);
        GridData gd_composite_hash = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
        gd_composite_hash.widthHint = 341;
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
        txtDuration.addListener(SWT.Verify, e-> e.doit = Pattern.matches("^[0-9]*$", e.text) );
        txtDuration.addListener(SWT.Modify, e-> {
            if( selectedPalMapping != null ) {
                selectedPalMapping.durationInMillis = Long.parseLong(txtDuration.getText());
                selectedPalMapping.durationInFrames = (int)selectedPalMapping.durationInMillis / 40;
            }
        });
        
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
        	PalMapping palMapping = new PalMapping(activePalette.index);
        	if( selectedHashIndex == -1 ) {
        		
        	} else {
        		palMapping.setDigest(hashes.get(selectedHashIndex));
        	}
        	palMapping.name = "KeyFrame "+(project.palMappings.size()+1);
        	palMapping.animationIndex = selectedAnimationIndex;
        	palMapping.frameIndex = selectedAnimation.actFrame;
        	project.palMappings.add(palMapping);
        	saveTimeCode = lastTimeCode;
        	keyframeListViewer.refresh();
        });
        new Label(grpKeyframe, SWT.NONE);
        new Label(grpKeyframe, SWT.NONE);
        
        btnFetchDuration = new Button(grpKeyframe, SWT.NONE);
        btnFetchDuration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnFetchDuration.setText("Fetch Duration");
        btnFetchDuration.setEnabled(false);
        btnFetchDuration.addListener(SWT.Selection, e->{
        	if( selectedPalMapping!=null) {
        		selectedPalMapping.durationInMillis = lastTimeCode - saveTimeCode;
                selectedPalMapping.durationInFrames = (int)selectedPalMapping.durationInMillis / FRAME_RATE;
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
        
        Group grpDetails = new Group(shell, SWT.NONE);
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
        
        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new GridLayout(9, false));
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
        	selectedAnimation.commitDMDchanges(dmd);
        	display.timerExec(animationHandler.getRefreshDelay(), cyclicRedraw);
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
        btnPrev.addListener(SWT.Selection, e-> {
        	animationHandler.prev();
        	selectedAnimation.commitDMDchanges(dmd);
        });
        
        btnNext = new Button(composite, SWT.NONE);
        btnNext.setText(">");
        btnNext.setEnabled(false);
        btnNext.addListener(SWT.Selection, e-> { 
        	animationHandler.next(); 
        	selectedAnimation.commitDMDchanges(dmd);
        });
        
        Button btnMarkStart = new Button(composite, SWT.NONE);
        Button btnMarkEnd = new Button(composite, SWT.NONE);
        Button btnCut = new Button(composite, SWT.NONE);

        btnMarkStart.setText("Mark Start");
        btnMarkStart.addListener(SWT.Selection, e->{
            cutStart = selectedAnimation.actFrame; 
            btnMarkEnd.setEnabled(true);
            });
        
        btnMarkEnd.setText("Mark End");
        btnMarkEnd.addListener(SWT.Selection, e->{
            cutEnd = selectedAnimation.actFrame;
            btnCut.setEnabled(true);
            });
        btnMarkEnd.setEnabled(false);
        
        btnCut.setText("Cut");
        btnCut.setEnabled(false);
        btnCut.addListener(SWT.Selection, e -> {
        	// respect number of planes while cutting / copying
            Animation ani = selectedAnimation.cutScene(cutStart, cutEnd, actualNumberOfPlanes);
            LOG.info("cutting out scene from {} to {}", cutStart, cutEnd);
            cutStart = 0; cutEnd = 0;
            btnMarkEnd.setEnabled(false);
            btnCut.setEnabled(false);
            ani.setDesc("Scene "+animations.size());
            animations.add(ani);
            aniListViewer.refresh();
            
            // TODO mark such a scene somehow, to copy it to the projects frames sequence for later export
            // alternatively introduce a dedicated flag for scenes that should be exported
            // also define a way that a keyframe triggers a replacement sequence instead of switching 
            // the palette only
            // TODO NEED TO ADD a reference to the animation in the list / map
            project.scenes.add( new Scene(ani.getDesc(), ani.start, ani.end, activePalette.index));
        });
        
        new Label(composite, SWT.NONE);
        
        Button btnUndo = new Button(composite, SWT.NONE);
        btnUndo.setText("Undo");        
        btnUndo.addListener(SWT.Selection, e->{
        	dmd.undo();
			dmdWidget.redraw();
        });
        ObserverManager.bind(dmd, e -> btnUndo.setEnabled(e), ()->dmd.canUndo() );
        
        Group grpPalettes = new Group(shell, SWT.NONE);
        grpPalettes.setLayout(new GridLayout(8, false));
        GridData gd_grpPalettes = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd_grpPalettes.heightHint = 90;
        grpPalettes.setLayoutData(gd_grpPalettes);
        grpPalettes.setText("Palettes");

        paletteComboViewer = new ComboViewer(grpPalettes, SWT.NONE);
        Combo combo = paletteComboViewer.getCombo();
        combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        paletteComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        paletteComboViewer.setLabelProvider(new PaletteViewerLabelProvider());
        paletteComboViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (selection.size() > 0) {
            	activePalette = (Palette) selection.getFirstElement();
                if( selectedPalMapping != null ) selectedPalMapping.palIndex = activePalette.index;
                dmdWidget.setPalette(activePalette);
                paletteTool.setPalette(activePalette);
                LOG.info("new palette is {}",activePalette);
                paletteTypeComboViewer.setSelection(new StructuredSelection(activePalette.type));
            }
        });

        paletteTypeComboViewer = new ComboViewer(grpPalettes, SWT.READ_ONLY);
        Combo combo_1 = paletteTypeComboViewer.getCombo();
        combo_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        paletteTypeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        paletteTypeComboViewer.setInput(PaletteType.values());
        paletteTypeComboViewer.setSelection(new StructuredSelection(PaletteType.NORMAL));
        paletteTypeComboViewer.addSelectionChangedListener(e -> {
            IStructuredSelection selection = (IStructuredSelection) e.getSelection();
            PaletteType palType = (PaletteType) selection.getFirstElement();
            activePalette.type = palType;
            if (PaletteType.DEFAULT.equals(palType)) {
                for (int i = 0; i < project.palettes.size(); i++) {
                    if (i != activePalette.index) { // set previous default to normal
                        if( project.palettes.get(i).type.equals(PaletteType.DEFAULT )) {
                        	project.palettes.get(i).type = PaletteType.NORMAL;
                        };
                    }
                }
            }
        })  ;

        btnNewPalette = new Button(grpPalettes, SWT.NONE);
        btnNewPalette.setText("New");
        btnNewPalette.addListener(SWT.Selection, e -> {
            String name = this.paletteComboViewer.getCombo().getText();
            if (!isNewPaletteName(name)) {
                name = "new" + UUID.randomUUID().toString().substring(0, 4);
            }
            activePalette = new Palette(activePalette.colors, project.palettes.size(), name);
            project.palettes.add(activePalette);
            paletteTool.setPalette(activePalette);
            paletteComboViewer.setSelection(new StructuredSelection(activePalette));
            paletteComboViewer.refresh();
        });

        btnRenamePalette = new Button(grpPalettes, SWT.NONE);
        btnRenamePalette.setText("Rename");
        btnRenamePalette.addListener(SWT.Selection, e -> {
        	activePalette.name = paletteComboViewer.getCombo().getText().split(" - ")[1];
        	paletteComboViewer.setSelection(new StructuredSelection(activePalette));
            paletteComboViewer.refresh();
        });

        Button btnReset = new Button(grpPalettes, SWT.NONE);
        btnReset.setText("Reset");
        btnReset.addListener(SWT.Selection, e -> {
            activePalette.setColors(Palette.defaultColors());
            paletteTool.setPalette(activePalette);
            dmdWidget.setPalette(activePalette);
        });

        Label lblPlanes = new Label(grpPalettes, SWT.NONE);
        lblPlanes.setText("Planes");

        ComboViewer planesComboViewer = new ComboViewer(grpPalettes, SWT.NONE);
        Combo planes = planesComboViewer.getCombo();
        GridData gd_planes = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_planes.widthHint = 60;
        planes.setLayoutData(gd_planes);
        planes.setItems(new String[] { "2", "4" });
        planes.setToolTipText("Number of planes");
        planes.select(1);
        planes.addListener(SWT.Selection, e -> {
        	actualNumberOfPlanes = numberOfPlanes [planes.getSelectionIndex()];
        	paletteTool.setNumberOfPlanes(actualNumberOfPlanes);
        });
        new Label(grpPalettes, SWT.NONE);

        paletteTool = new PaletteTool(grpPalettes, SWT.FLAT | SWT.RIGHT, activePalette);
        
        drawTools.put("pencil", new SetPixelTool(paletteTool.getSelectedColor()));       
        drawTools.put("fill", new FloodFillTool(paletteTool.getSelectedColor()));       
        drawTools.put("rect", new RectTool(paletteTool.getSelectedColor()));
        drawTools.put("line", new LineTool(paletteTool.getSelectedColor()));
        drawTools.put("circle", new CircleTool(paletteTool.getSelectedColor()));
        drawTools.values().forEach(d->paletteTool.addListener(d));
        
        btnChangeColor = new Button(grpPalettes, SWT.NONE);
        btnChangeColor.setText("Color");
		btnChangeColor.addListener(SWT.Selection, e -> {
			ColorDialog cd = new ColorDialog(shell);
			cd.setText("Select new color");
			cd.setRGB(paletteTool.getSelectedRGB());
			RGB rgb = cd.open();
			if (rgb == null) {
				return;
			}
			activePalette.colors[paletteTool.getSelectedColor()] = new RGB(rgb.red, rgb.green, rgb.blue);
			dmdWidget.setPalette(activePalette);
			paletteTool.setPalette(activePalette);
		});

        ToolBar toolBar = new ToolBar(grpPalettes, SWT.FLAT | SWT.RIGHT);
        toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
        
        ToolItem tltmPen = new ToolItem(toolBar, SWT.RADIO);
        tltmPen.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/pencil.png")));
        tltmPen.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("pencil")));
        
        ToolItem tltmFill = new ToolItem(toolBar, SWT.RADIO);
        tltmFill.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/color-fill.png")));
        tltmFill.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("fill")));
        
        ToolItem tltmRect = new ToolItem(toolBar, SWT.RADIO);
        tltmRect.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/rect.png")));
        tltmRect.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("rect")));
        
        ToolItem tltmLine = new ToolItem(toolBar, SWT.RADIO);
        tltmLine.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/line.png")));
        tltmLine.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("line")));

        ToolItem tltmCircle = new ToolItem(toolBar, SWT.RADIO);
        tltmCircle.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/oval.png")));
        tltmCircle.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("circle")));

//        ToolItem tltmEraser = new ToolItem(toolBar, SWT.RADIO);
//        tltmEraser.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/eraser.png")));
//        tltmEraser.addListener(SWT.Selection, e->dmdWidget.setDrawTool(null));
        
        Button btnUploadFrame = new Button(grpPalettes, SWT.NONE);
        btnUploadFrame.setText("Upload Frame");
        
        Button btnUploadPalette = new Button(grpPalettes, SWT.NONE);
        btnUploadPalette.setText("Upload Palette");
        btnUploadPalette.addListener(SWT.Selection, e->usbTool.upload(activePalette));
        
        Button btnUploadMappings = new Button(grpPalettes, SWT.NONE);
        btnUploadMappings.setText("Upload KeyFrames");
        btnUploadMappings.addListener(SWT.Selection, e->usbTool.upload(project.palMappings));
        
        new Label(grpPalettes, SWT.NONE);
        new Label(grpPalettes, SWT.NONE);
        new Label(grpPalettes, SWT.NONE);
        new Label(grpPalettes, SWT.NONE);
        new Label(grpPalettes, SWT.NONE);
        new Label(grpPalettes, SWT.NONE);
        new Label(grpPalettes, SWT.NONE);
        new Label(grpPalettes, SWT.NONE);
        new Label(grpPalettes, SWT.NONE);

    }


	/**
	 * check if dirty.
	 * @return true, if not dirty or if user decides to ignore dirtyness
	 */
	boolean dirtyCheck() {
		if( project.dirty ) {
			MessageBox messageBox = new MessageBox(shell,
                    SWT.ICON_WARNING | SWT.OK | SWT.CANCEL  );
            
            messageBox.setText("Unsaved Changes");
            messageBox.setMessage("There are unsaved changes in project. Proceed?");
            int res = messageBox.open();
            return (res == SWT.OK);
		} else {
			return true;
		}
	}
	
	/**
	 * creates the top level menu
	 */
	private void createMenu(Shell shell) {
		Menu menu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menu);
		
		MenuItem mntmfile = new MenuItem(menu, SWT.CASCADE);
		mntmfile.setText("&File");
		
		Menu menu_1 = new Menu(mntmfile);
		mntmfile.setMenu(menu_1);
		
		MenuItem mntmNewProject = new MenuItem(menu_1, SWT.NONE);
		mntmNewProject.setText("New Project");
		mntmNewProject.addListener(SWT.Selection, e->{ 
			if( dirtyCheck() ) {
				createNewProject();
			}
		} );
		
		MenuItem mntmLoadProject = new MenuItem(menu_1, SWT.NONE);
		mntmLoadProject.addListener(SWT.Selection, e-> loadProject());
		mntmLoadProject.setText("Load Project");
		
		MenuItem mntmSaveProject = new MenuItem(menu_1, SWT.NONE);
		mntmSaveProject.setText("Save Project");
		mntmSaveProject.addListener(SWT.Selection, e->saveProject());
		
		new MenuItem(menu_1, SWT.SEPARATOR);
		
		MenuItem mntmExit = new MenuItem(menu_1, SWT.NONE);
		mntmExit.setText("Exit");
		mntmExit.addListener(SWT.Selection, e->{
			// TODO add dirty check
			shell.close();
            shell.dispose();
		});
		
		MenuItem mntmedit = new MenuItem(menu, SWT.CASCADE);
		mntmedit.setText("&Edit");
		
		Menu menu_5 = new Menu(mntmedit);
		mntmedit.setMenu(menu_5);
		
		MenuItem mntmUndo = new MenuItem(menu_5, SWT.NONE);
		mntmUndo.setText("Undo");
		mntmUndo.addListener(SWT.Selection, e-> {
			dmd.undo();
			dmdWidget.redraw();
		});
		ObserverManager.bind(dmd, e->mntmUndo.setEnabled(e), () -> dmd.canUndo());
		
		MenuItem mntmRedo = new MenuItem(menu_5, SWT.NONE);
		mntmRedo.setText("Redo");
		mntmRedo.addListener(SWT.Selection, e->{
			dmd.redo();
			dmdWidget.redraw();
		});
		ObserverManager.bind(dmd, e->mntmRedo.setEnabled(e), ()->dmd.canRedo() );
		
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
		mntmLoadPalette.addListener(SWT.Selection, e->loadPalette());
		
		MenuItem mntmSavePalette = new MenuItem(menu_3, SWT.NONE);
		mntmSavePalette.setText("Save Palette");
		mntmSavePalette.addListener(SWT.Selection, e->savePalette());
		
		MenuItem mntmhelp = new MenuItem(menu, SWT.CASCADE);
		mntmhelp.setText("&Help");
		
		Menu menu_4 = new Menu(mntmhelp);
		mntmhelp.setMenu(menu_4);
		
		MenuItem mntmGetHelp = new MenuItem(menu_4, SWT.NONE);
		mntmGetHelp.setText("Get help");
		mntmGetHelp.addListener(SWT.Selection, e->Program.launch(HELP_URL));
		
		new MenuItem(menu_4, SWT.SEPARATOR);
		
		MenuItem mntmAbout = new MenuItem(menu_4, SWT.NONE);
		mntmAbout.setText("About");
		mntmAbout.addListener(SWT.Selection, e->new About(shell).open());
	}

	public String getPrintableHashes(byte[] p) {
		StringBuffer hexString = new StringBuffer();
		for (int j = 0; j < p.length; j++)
			hexString.append(String.format("%02X", p[j]));
		return hexString.toString();
	}

	private boolean isNewPaletteName(String text) {
		for (Palette pal : project.palettes) {
			if (pal.name.equals(text))
				return false;
		}
		return true;
	}
}
