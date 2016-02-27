package com.rinke.solutions.pinball;

import java.awt.SplashScreen;

import static com.rinke.solutions.pinball.api.LicenseManager.Capability;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
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
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinke.solutions.pinball.animation.AniEvent;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationCompiler;
import com.rinke.solutions.pinball.animation.AnimationFactory;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.EventHandler;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.io.SmartDMDImporter;
import com.rinke.solutions.pinball.io.UsbTool;
import com.rinke.solutions.pinball.model.FrameSeq;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.PlaneNumber;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.model.Scene;
import com.rinke.solutions.pinball.ui.About;
import com.rinke.solutions.pinball.ui.DeviceConfig;
import com.rinke.solutions.pinball.ui.FileChooser;
import com.rinke.solutions.pinball.ui.FileDialogDelegate;
import com.rinke.solutions.pinball.ui.GifExporter;
import com.rinke.solutions.pinball.ui.RegisterLicense;
import com.rinke.solutions.pinball.api.BinaryExporter;
import com.rinke.solutions.pinball.api.BinaryExporterFactory;
import com.rinke.solutions.pinball.api.LicenseManager;
import com.rinke.solutions.pinball.api.LicenseManagerFactory;
import com.rinke.solutions.pinball.util.ObservableList;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.widget.CircleTool;
import com.rinke.solutions.pinball.widget.DMDWidget;
import com.rinke.solutions.pinball.widget.DrawTool;
import com.rinke.solutions.pinball.widget.FloodFillTool;
import com.rinke.solutions.pinball.widget.LineTool;
import com.rinke.solutions.pinball.widget.PaletteTool;
import com.rinke.solutions.pinball.widget.RectTool;
import com.rinke.solutions.pinball.widget.SetPixelTool;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class PinDmdEditor implements EventHandler{

	private static final Logger LOG = LoggerFactory.getLogger(PinDmdEditor.class);
	
	private static final int FRAME_RATE = 40;

	private static final String HELP_URL = "http://go-dmd.de/2015/11/24/pin2dmd-editor/";

	DMD dmd = new DMD(128,32);
	
	AnimationHandler animationHandler = null;
	
	CyclicRedraw cyclicRedraw = new CyclicRedraw();
	
	ObservableMap<String,Animation> animations = new ObservableMap<String,Animation>(new LinkedHashMap<>());
    Map<String,DrawTool> drawTools = new HashMap<>();

    Display display;
	protected Shell shell;

	protected int lastTimeCode;

	@Option(name="-ani", usage="animation file to load", required=false)
	private String aniToLoad;
	
	@Option(name="-cut", usage="<src name>,<new name>,<start>,<end>", required=false)
	private String cutCmd;

	@Option(name="-nodirty", usage="dont check dirty flag on close", required=false)
	private boolean nodirty=false;

	@Option(name="-save", usage="if set, project is saved right away", required=false)
	private String saveFile;
	
    @Argument
    private java.util.List<String> arguments = new ArrayList<String>();

    private Label lblTcval;
	private Label lblFrameNo;
	
	private String lastPath;
	private String frameTextPrefix = "Pin2dmd Editor ";
	private Animation defaultAnimation = new Animation(null, "", 0, 0, 1, 1, 1);
	Optional<Animation> selectedAnimation = Optional.of(defaultAnimation);
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
	Scale scale;
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
	ToolBar drawToolBar;
    ComboViewer frameSeqViewer;
    Button btnMarkStart;
    Button btnMarkEnd;
    Button btnCut;
    Button btnStart;
    Button btnStop;
	Button btnAddFrameSeq;
	DMDWidget previewDmd;
    ObservableList<Animation> frameSeqList = new  ObservableList<>(new ArrayList<>());
	ComboViewer planesComboViewer;
    
	PaletteTool paletteTool;
	int selectedHashIndex;
	PalMapping selectedPalMapping;
	int saveTimeCode;
	
	CutInfo cutInfo = new CutInfo();

	java.util.List<Palette> previewPalettes = new ArrayList<>();

	PlaneNumber planeNumer;

    Label lblPlanesVal;

    Label lblDelayVal;

	private Button btnSortAni;

	LicenseManager licManager;


	public PinDmdEditor() {
		super();
	    activePalette = project.palettes.get(0);
	    previewPalettes = Palette.previewPalettes();
	    licManager = LicenseManagerFactory.getInstance();
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
			if( previewDmd!=null && !previewDmd.isDisposed() ) previewDmd.redraw();
            if (animationHandler != null && !animationHandler.isStopped()) {
            	animationHandler.run();
                display.timerExec(animationHandler.getRefreshDelay(), cyclicRedraw);
            }
		}
    }

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			PinDmdEditor window = new PinDmdEditor();
			window.open(args);
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
    
    public void createBindings() {
		// do some bindings
        ObserverManager.bind(animationHandler, e->drawToolBar.setEnabled(e), ()->animationHandler.isStopped());
        ObserverManager.bind(animationHandler, e->dmdWidget.setDrawingEnabled(e), ()->animationHandler.isStopped());

        ObserverManager.bind(animationHandler, e->btnStop.setEnabled(e), ()->!animationHandler.isStopped());
        ObserverManager.bind(animationHandler, e->btnStart.setEnabled(e), ()->animationHandler.isStopped());

        ObserverManager.bind(animationHandler, e->btnPrev.setEnabled(e), ()->animationHandler.isStopped());
        ObserverManager.bind(animationHandler, e->btnNext.setEnabled(e), ()->animationHandler.isStopped());

        ObserverManager.bind(cutInfo, e->btnCut.setEnabled(e),
                ()-> ( cutInfo.getStart()>0 && cutInfo.getEnd()>0 ));

        ObserverManager.bind(cutInfo, e->btnMarkEnd.setEnabled(e),
                ()-> ( cutInfo.getStart()>0  ));
        
        ObserverManager.bind(animations, e->btnStart.setEnabled(e), 
        		()->!this.animations.isEmpty()&&animationHandler.isStopped());
        ObserverManager.bind(animations, e->btnPrev.setEnabled(e), ()->!this.animations.isEmpty());
        ObserverManager.bind(animations, e->btnNext.setEnabled(e), ()->!this.animations.isEmpty());
        ObserverManager.bind(animations, e->btnMarkStart.setEnabled(e), ()->!this.animations.isEmpty());

        ObserverManager.bind(animations, e->aniListViewer.refresh(), ()->true);
        ObserverManager.bind(animations, e->buildFrameSeqList(), ()->true);
        
        //ObserverManager.bind(animations, e->btnAddFrameSeq.setEnabled(e), ()->!frameSeqList.isEmpty());
    }
    
    protected void buildFrameSeqList() {
    	frameSeqList.clear();
    	frameSeqList.addAll( animations.values().stream().filter(a->!a.isLoadedFromFile()).collect(Collectors.toList()));
    	frameSeqViewer.refresh();
    }

	/**
	 * Open the window.
	 * @param args 
	 */
	public void open(String[] args) {
	    
	    CmdLineParser parser = new CmdLineParser(this);
	    try{
	        parser.parseArgument(args);
	    } catch( CmdLineException e) {
	        System.err.println(e.getMessage());
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
            System.exit(1);
	    }
	    
	    display = Display.getDefault();
		shell = new Shell();
		
		createContents(shell);

		createNewProject();
		
		paletteComboViewer.getCombo().select(0);
		paletteTool.setPalette(activePalette);
		
		animationHandler = new AnimationHandler(playingAnis, clock, dmd);
		
        animationHandler.setScale(scale);
		animationHandler.setEventHandler(this);
		    
		createBindings();
		
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
		
		// cmd line processing
		if( aniToLoad != null ) {
		    loadAni(aniToLoad, false, true);
		}
		if( cutCmd != null && !animations.isEmpty() ) {
		    String[] cuts = cutCmd.split(",");
		    if( cuts.length >=3 ) {
		        cutScene(animations.get(cuts[0]),Integer.parseInt(cuts[2]), Integer.parseInt(cuts[3]), cuts[1]);
		    }
		}
		if( saveFile != null ) {
			saveProject(saveFile);
		}
		
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
	
    private Animation cutScene(Animation animation, int start, int end, String name) {
        Animation cutScene = animation.cutScene( 
                start,
                end, 4);
        //TODO improve to make it selectable how many planes
        cutScene.setDesc(name);
        animations.put(name,cutScene);
        aniListViewer.setSelection(new StructuredSelection(cutScene));
        
        return cutScene;
    }

    void createNewProject() {
    	project.clear();
	    activePalette = project.palettes.get(0);
	    paletteComboViewer.refresh();
    	keyframeListViewer.refresh();
	    animations.clear();
	    playingAnis.clear();
	    selectedAnimation = Optional.of(defaultAnimation);
	}

	private void loadProject() {
        String filename = fileChooserHelper(SWT.OPEN, null, 
        		new String[] { "*.xml;*.json;" },
        		new String[] { "Project XML", "Project JSON" });

        if (filename != null)  loadProject(filename);
    }
	
	/** 
	 * imports a secondary project to implement a merge functionality
	 */
	void importProject() {
        String filename = fileChooserHelper(SWT.OPEN, null, 
        		new String[] { "*.xml;*.json;" },
        		new String[] { "Project XML", "Project JSON" });

        if (filename != null)  importProject(filename);
	}
	
	void importProject(String filename) {
        LOG.info("importing project from {}",filename);
        Project projectToImport  = (Project) fileHelper.loadObject(filename);
		// merge into existing Project
        HashSet<String> collisions = new HashSet<>();
        for( String key: projectToImport.frameSeqMap.keySet()) {
        	if( project.frameSeqMap.containsKey(key)) {
        		collisions.add(key);
        	} else {
        		project.frameSeqMap.put(key, projectToImport.frameSeqMap.get(key));
        	}
        }
        if( !collisions.isEmpty() ) {
            MessageBox messageBox = new MessageBox(shell,
                    SWT.ICON_WARNING | SWT.OK | SWT.IGNORE | SWT.ABORT  );
            
            messageBox.setText("Override warning");
            messageBox.setMessage("the following frame seq have NOT been \nimported due to name collisions: "+collisions+
                    "\n");
            messageBox.open();
        }
        
        for( String inputFile : projectToImport.inputFiles ) {
        	loadAni(buildRelFilename(filename, inputFile), true, true);
        }
        for( PalMapping palMapping : projectToImport.palMappings) {
        	project.palMappings.add(palMapping);
        }
	}


	void loadProject(String filename) {
        LOG.info("load project from {}",filename);
        Project projectToLoad  = (Project) fileHelper.loadObject(filename);

        if( projectToLoad != null ) {
        	shell.setText(frameTextPrefix+" - "+new File(filename).getName());
            project = projectToLoad;
            
            for( String file : project.inputFiles) loadAni(buildRelFilename(filename,file), true, false);
            
            for( int i = 1; i < project.scenes.size(); i++) {
            	//cutOutNewAnimation(project.scenes.get(i).start, project.scenes.get(i).end, animations.get(0));
            	LOG.info("cutting out "+project.scenes.get(i));
            }
            
            paletteComboViewer.setInput(project.palettes);
            keyframeListViewer.setInput(project.palMappings);
            for( Animation ani : animations.values()) {
            	selectedAnimation = Optional.of(animations.isEmpty() ? defaultAnimation : ani);
            	break;
            }
            
        }
		
	}
	
	private String buildRelFilename(String parent, String file) {
		return new File(parent).getParent()+File.separator+file;
	}


	private void exportProject() {
        String filename = fileChooserHelper(SWT.SAVE, project.name, 
        		new String[] { "*.dat" },
        		new String[] { "Export dat" });	
        if( filename != null ) exportProject(filename);
	}

    private void saveProject() {
        String filename = fileChooserHelper(SWT.SAVE, project.name, 
        		new String[] { "*.xml", "*.json"},
        		new String[] { "Project XML", "Project JSON" });
        if (filename != null) saveProject(filename);
    }
    
    void exportProject(String filename) {
        
    	licManager.requireOneOf( Capability.VPIN, Capability.REALPIN );
    	
        for( PalMapping p : project.palMappings) {
            if( p.frameSeqName != null ) {
                project.frameSeqMap.put(p.frameSeqName, new FrameSeq(p.frameSeqName) );
            }
        }
    	
    	// for all referenced frame mapping we must also copy the frame data as there are two models
    	for( FrameSeq p: project.frameSeqMap.values() ) {
    		Animation ani = animations.get(p.name);
    		ani.actFrame = 0;
    		DMD tmp = new DMD(128,32);
    		for (int i = 0; i <= ani.end; i++) {
    			Frame frame = ani.render(tmp, false);
    			p.frames.add(frame);
    		}
    	}
    
    	// create addtional files for frame sequences
    	try {
    		Map<String, Integer> map = new HashMap<String, Integer>();
            BinaryExporter exporter = BinaryExporterFactory.getInstance();
    		if( !project.frameSeqMap.isEmpty() ) {
        		DataOutputStream dos = new DataOutputStream(new FileOutputStream(replaceExtensionTo("fsq",filename)));
        		map = exporter.writeFrameSeqTo(dos, project);
        		dos.close();
    		}
    		
            DataOutputStream dos2 = new DataOutputStream(new FileOutputStream(filename));
            exporter.writeTo(dos2, map, project);
            dos2.close();
        	//fileHelper.storeObject(project, filename);
		} catch (IOException e) {
			throw new RuntimeException("error writing "+filename,e);
		}
	}

	private void saveProject(String filename) {
        LOG.info("write project to {}",filename);
        String aniFilename = replaceExtensionTo("ani", filename);
        int numberOfStoredAnis = storeAnimations(animations.values(), aniFilename);
        if( numberOfStoredAnis > 0 ) {
        	project.inputFiles.add(new File(aniFilename).getName());
        }
        fileHelper.storeObject(project, filename);
        project.dirty = false;
    }
	
    private void saveAniWithFC()
    {
        String filename = fileChooserHelper(SWT.SAVE, activePalette.name, 
        		new String[] { "*.ani" }, new String[] { "Animations" });
        if (filename != null) {
            LOG.info("store animation to {}",filename);
            storeAnimations(this.animations.values(), filename);
        }
    }

    private int storeAnimations(Collection<Animation> anis, String filename) {
		java.util.List<Animation> anisToSave = anis.stream().filter(a->!a.isLoadedFromFile()).collect(Collectors.toList());
		AnimationCompiler.writeToCompiledFile(anisToSave, filename);
		return anisToSave.size();
    }


	String replaceExtensionTo(String newExt, String filename) {
		int p = filename.lastIndexOf(".");
		if( p!=-1) return filename.substring(0,p)+"."+newExt;
		return filename;
	}


	protected void loadAniWithFC(boolean append) {
        String filename = fileChooserHelper(SWT.OPEN, null, 
        		new String[] { "*.properties;*.ani;*.txt.gz;*.pcap;*.pcap.gz;*.*" },
        		new String[] { "Animationen", "properties, txt.gz, ani, mov" });

        if (filename != null) {
            loadAni(filename, append, true);
        }
    }
	
	boolean extensionIs( String name, String ... args ) {
		for( String ext : args) {
			if(name.endsWith(ext)) return true;
		}
		return false;
	}
    
    public void loadAni(String filename, boolean append, boolean populateProject) {
        java.util.List<Animation> loadedList = new ArrayList<>();
        if (filename.endsWith(".ani")) {
            loadedList.addAll(AnimationCompiler.readFromCompiledFile(filename));
        } else if (filename.endsWith(".txt.gz")) {
            loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.MAME));
        } else if (filename.endsWith(".properties")) {
            loadedList.addAll(AnimationFactory.createAnimationsFromProperties(filename));
        } else if (extensionIs(filename, ".pcap",".pcap.gz") ) {
        	loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.PCAP));
        } else if ( extensionIs(filename, ".mp4", ".3gp", ".avi" ) ) {
        	loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.VIDEO));
        }
        LOG.info("loaded {} animations from {}", loadedList.size(), filename);
        
        if( populateProject ) {
            if( !append ) project.inputFiles.clear();
            if( !project.inputFiles.contains(filename) ) project.inputFiles.add(filename);
        }
        
        // animationHandler.setAnimations(sourceAnis);
        if (!append) {
            animations.clear();
            playingAnis.clear();
        }
        for( Animation ani : loadedList) {
        	if( animations.containsKey(ani.getDesc()) ) {
        		int i = 0;
        		String desc = ani.getDesc();
        		while(i<1000) {
        			String newDesc = desc+"-"+i;
        			if(!animations.containsKey(newDesc)) {
        				ani.setDesc(newDesc);
        				break;
        			}
        			i++;
        		}
        	}
        	animations.put(ani.getDesc(), ani);
        }
        
        project.dirty = true;
    }
    
    String fileChooserHelper(int type, String filename, String[] exts, String[] desc ) {
        FileChooser fileChooser = createFileChooser(shell, type);
        fileChooser.setOverwrite(true);
        fileChooser.setFileName(filename);
        if (lastPath != null)
            fileChooser.setFilterPath(lastPath);
        fileChooser.setFilterExtensions(exts);
        fileChooser.setFilterNames(desc);
        String returnedFilename = fileChooser.open();
        lastPath = fileChooser.getFilterPath();
    	return returnedFilename;
    }

    private void savePalette()
    {
        String filename = fileChooserHelper(SWT.SAVE, activePalette.name, 
        		new String[] { "*.xml", "*.json" }, new String[] { "Paletten XML", "Paletten JSON" });
        if (filename != null) {
            LOG.info("store palette to {}",filename);
            fileHelper.storeObject(activePalette, filename);
        }
    }
    
    private void loadPalette() {
    	String filename = fileChooserHelper(SWT.OPEN, null, 
        		new String[] { "*.xml","*.json,", "*.txt" }, new String[] { "Palette XML", "Palette JSON", "smartdmd" });
        if (filename != null) loadPalette(filename);
    }
    
    void loadPalette(String filename) {
		if (filename.toLowerCase().endsWith(".txt")) {
			java.util.List<Palette> palettesImported = smartDMDImporter
					.importFromFile(filename);
			String override = checkOverride(project.palettes, palettesImported);
			if (!override.isEmpty()) {
				MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING
						| SWT.OK | SWT.IGNORE | SWT.ABORT);

				messageBox.setText("Override warning");
				messageBox
						.setMessage("importing these palettes will override palettes: "
								+ override + "\n");
				int res = messageBox.open();
				if (res != SWT.ABORT) {
					importPalettes(palettesImported, res == SWT.OK);
				}
			} else {
				importPalettes(palettesImported, true);
			}
		} else {
			Palette pal = (Palette) fileHelper.loadObject(filename);
			LOG.info("load palette from {}", filename);
			project.palettes.add(pal);
			activePalette = pal;
		}
		paletteComboViewer.setSelection(new StructuredSelection(activePalette));
		paletteComboViewer.refresh();
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
    
    void importPalettes(java.util.List<Palette> palettesImported, boolean override) {
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

    String checkOverride(java.util.List<Palette> palettes2, java.util.List<Palette> palettesImported) {
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
            	// switch palettes in preview
            	previewDmd.setPalette(previewPalettes.get(selectedHashIndex));
            });
    	}
    }
    

	/**
	 * Create contents of the window.
	 */
	 void createContents(Shell shell) {
		shell.setSize(1167, 580);
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
		aniListViewer.setLabelProvider(new LabelProviderAdapter(o->((Animation)o).getDesc()));
		aniListViewer.setInput(animations.values());
		aniListViewer.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (selection.size() > 0){
            	selectedAnimation = Optional.of((Animation)selection.getFirstElement());
            	int numberOfPlanes = selectedAnimation.get().getRenderer().getNumberOfPlanes();
            	if( numberOfPlanes == 3 ) numberOfPlanes = 2;
            	dmd.setNumberOfSubframes(numberOfPlanes);
            	planesComboViewer.setSelection(new StructuredSelection(PlaneNumber.valueOf(numberOfPlanes)));
                playingAnis.clear();
                playingAnis.add(selectedAnimation.get());
                animationHandler.setAnimations(playingAnis);
                dmdRedraw(); 
            } else {
            	selectedAnimation = Optional.of(defaultAnimation);
            }
            btnRemoveAni.setEnabled(selection.size()>0);
            btnAddKeyframe.setEnabled(selection.size()>0);
		});
		
		keyframeListViewer = new ListViewer(shell, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		List keyframeList = keyframeListViewer.getList();
		GridData gd_keyframeList = new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1);
		gd_keyframeList.widthHint = 137;
		keyframeList.setLayoutData(gd_keyframeList);
		keyframeListViewer.setLabelProvider(new LabelProviderAdapter(o->((PalMapping)o).name));
		keyframeListViewer.setContentProvider(ArrayContentProvider.getInstance());
		keyframeListViewer.setInput(project.palMappings);
		keyframeListViewer.addSelectionChangedListener(event -> keyFrameChanged(event));
		
        dmdWidget = new DMDWidget(shell, SWT.DOUBLE_BUFFERED, this.dmd);
        //dmdWidget.setBounds(0, 0, 700, 240);
        GridData gd_dmdWidget = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd_dmdWidget.heightHint = 210;
        gd_dmdWidget.widthHint = 790;
        dmdWidget.setLayoutData(gd_dmdWidget);
        dmdWidget.setPalette(activePalette);
        
        Composite composite_1 = new Composite(shell, SWT.NONE);
        composite_1.setLayout(new GridLayout(2, false));
        GridData gd_composite_1 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_composite_1.heightHint = 35;
        gd_composite_1.widthHint = 206;
        composite_1.setLayoutData(gd_composite_1);
        
        btnRemoveAni = new Button(composite_1, SWT.NONE);
        btnRemoveAni.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        btnRemoveAni.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        	}
        });
        btnRemoveAni.setText("Remove");
        btnRemoveAni.setEnabled(false);
        btnRemoveAni.addListener(SWT.Selection, e->{
            if( selectedAnimation.isPresent() ) {
            	String key = selectedAnimation.get().getDesc();
                animations.remove(key);
                playingAnis.clear();
                animationHandler.setAnimations(playingAnis);
                animationHandler.setClockActive(true);
            }
        });
        
        btnSortAni = new Button(composite_1, SWT.NONE);
        btnSortAni.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        btnSortAni.setText("Sort");
        btnSortAni.addListener(SWT.Selection, e->sortAnimations() );
        
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
        
        previewDmd = new DMDWidget(grpKeyframe, SWT.DOUBLE_BUFFERED, dmd);
        GridData gd_dmdPreWidget = new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 2);
        gd_dmdPreWidget.heightHint = 40;
        //gd_dmdPreWidget.heightHint = 40;
        gd_dmdPreWidget.widthHint = 199;
        previewDmd.setLayoutData(gd_dmdPreWidget);
        previewDmd.setDrawingEnabled(false);
		previewDmd.setPalette(previewPalettes.get(0));

        new Label(grpKeyframe, SWT.NONE);
        
        btnAddKeyframe = new Button(grpKeyframe, SWT.NONE);
        btnAddKeyframe.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1));
        btnAddKeyframe.setText("Add PalSwitch");
        btnAddKeyframe.setEnabled(false);
        btnAddKeyframe.addListener(SWT.Selection, e->{
        	PalMapping palMapping = new PalMapping(activePalette.index, "KeyFrame "+(project.palMappings.size()+1));
        	if( selectedHashIndex != -1 ) {
        		palMapping.setDigest(hashes.get(selectedHashIndex));
        	}
        	palMapping.animationName = selectedAnimation.get().getDesc();
        	palMapping.frameIndex = selectedAnimation.get().actFrame;
        	palMapping.switchMode = SwitchMode.PALETTE;
        	project.palMappings.add(palMapping);
        	saveTimeCode = lastTimeCode;
        	keyframeListViewer.refresh();
        });
        new Label(grpKeyframe, SWT.NONE);
        new Label(grpKeyframe, SWT.NONE);
        
        btnDeleteKeyframe = new Button(grpKeyframe, SWT.NONE);
        GridData gd_btnDeleteKeyframe = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_btnDeleteKeyframe.widthHint = 104;
        btnDeleteKeyframe.setLayoutData(gd_btnDeleteKeyframe);
        btnDeleteKeyframe.setText("Del KeyFrame");
        btnDeleteKeyframe.setEnabled(false);
        btnDeleteKeyframe.addListener(SWT.Selection, e->{
        	if( selectedPalMapping!=null) {
        		project.palMappings.remove(selectedPalMapping);
        		keyframeListViewer.refresh();
        	}
        });
        
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
                selectedPalMapping.durationInMillis = Integer.parseInt(txtDuration.getText());
                selectedPalMapping.durationInFrames = (int)selectedPalMapping.durationInMillis / 40;
            }
        });
        
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
        
        Label lblNewLabel = new Label(grpKeyframe, SWT.NONE);
        lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblNewLabel.setText("FrameSeq");
        
        frameSeqViewer = new ComboViewer(grpKeyframe, SWT.NONE);
        Combo frameSeqCombo = frameSeqViewer.getCombo();
        frameSeqCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        frameSeqViewer.setLabelProvider(new LabelProviderAdapter(o->((Animation)o).getDesc()));
        frameSeqViewer.setContentProvider(ArrayContentProvider.getInstance());
        frameSeqViewer.setInput(frameSeqList);
        
        btnAddFrameSeq = new Button(grpKeyframe, SWT.NONE);
        btnAddFrameSeq.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnAddFrameSeq.setText("Add FrameSeq");
        btnAddFrameSeq.addListener(SWT.Selection, e->{
        	if( !frameSeqViewer.getSelection().isEmpty()) {
        		Animation ani = (Animation) ((IStructuredSelection) frameSeqViewer.getSelection()).getFirstElement();
        		// TODO add index, add ref to framesSeq
        		PalMapping palMapping = new PalMapping(0, "KeyFrame "+ani.getDesc());
            	if( selectedHashIndex != -1 ) {
            		palMapping.setDigest(hashes.get(selectedHashIndex));
            	}
        		palMapping.palIndex = activePalette.index;
        		palMapping.frameSeqName = ani.getDesc();
        		palMapping.animationName = ani.getDesc();
            	palMapping.switchMode = SwitchMode.REPLACE;
        		palMapping.frameIndex = selectedAnimation.get().actFrame;
        		project.palMappings.add(palMapping);
        		keyframeListViewer.refresh();
        	}
        });

        Group grpDetails = new Group(shell, SWT.NONE);
        grpDetails.setLayout(new GridLayout(8, false));
        GridData gd_grpDetails = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd_grpDetails.heightHint = 21;
        gd_grpDetails.widthHint = 231;
        grpDetails.setLayoutData(gd_grpDetails);
        grpDetails.setText("Details");
        
        Label lblFrame = new Label(grpDetails, SWT.NONE);
        lblFrame.setText("Frame:");
        
        lblFrameNo = new Label(grpDetails, SWT.NONE);
        GridData gd_lblFrameNo = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblFrameNo.widthHint = 66;
        gd_lblFrameNo.minimumWidth = 60;
        lblFrameNo.setLayoutData(gd_lblFrameNo);
        lblFrameNo.setText("xxxxx");
        
        Label lblTimecode = new Label(grpDetails, SWT.NONE);
        lblTimecode.setText("Timecode:");
        
        lblTcval = new Label(grpDetails, SWT.NONE);
        GridData gd_lblTcval = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblTcval.widthHint = 62;
        gd_lblTcval.minimumWidth = 80;
        lblTcval.setLayoutData(gd_lblTcval);
        lblTcval.setText("xxxxx");
        
        Label lblDelay = new Label(grpDetails, SWT.NONE);
        lblDelay.setText("Delay:");
        
        lblDelayVal = new Label(grpDetails, SWT.NONE);
        lblDelayVal.setText("xxxxx");
        
        Label lblPlanes = new Label(grpDetails, SWT.NONE);
        lblPlanes.setText("Planes:");
        
        lblPlanesVal = new Label(grpDetails, SWT.NONE);
        lblPlanesVal.setText("xxxxxx");

        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new GridLayout(10, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        
        btnStart = new Button(composite, SWT.NONE);
        btnStop = new Button(composite, SWT.NONE);
        btnStart.setText("Start");
        btnStart.addListener(SWT.Selection, e-> {
        	selectedAnimation.orElse(defaultAnimation).commitDMDchanges(dmd);
        	animationHandler.start();
        	display.timerExec(animationHandler.getRefreshDelay(), cyclicRedraw);
        });

        btnStop.setText("Stop");
        btnStop.addListener(SWT.Selection, e->{
        	animationHandler.stop();
        });
        
        btnPrev = new Button(composite, SWT.NONE);
        btnPrev.setText("<");
        btnPrev.addListener(SWT.Selection, e-> {
        	selectedAnimation.orElse(defaultAnimation).commitDMDchanges(dmd);
        	animationHandler.prev();
        });
        
        btnNext = new Button(composite, SWT.NONE);
        btnNext.setText(">");
        btnNext.addListener(SWT.Selection, e-> { 
        	selectedAnimation.orElse(defaultAnimation).commitDMDchanges(dmd);
        	animationHandler.next(); 
        });
        
        btnMarkStart = new Button(composite, SWT.NONE);
        btnMarkEnd = new Button(composite, SWT.NONE);
        btnCut = new Button(composite, SWT.NONE);

        btnMarkStart.setText("Mark Start");
        btnMarkStart.addListener(SWT.Selection, e->{
            cutInfo.setStart(selectedAnimation.get().actFrame); 
            });
        
        btnMarkEnd.setText("Mark End");
        btnMarkEnd.addListener(SWT.Selection, e->{
            cutInfo.setEnd(selectedAnimation.get().actFrame);
        });
        
        btnCut.setText("Cut");
        btnCut.addListener(SWT.Selection, e -> {
        	// respect number of planes while cutting / copying
            Animation ani = cutScene(selectedAnimation.get(), cutInfo.getStart(), cutInfo.getEnd(), "Scene "+animations.size());
            LOG.info("cutting out scene from {} to {}", cutInfo );
            cutInfo.reset();

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
			dmdRedraw();
        });
        ObserverManager.bind(dmd, e -> btnUndo.setEnabled(e), ()->dmd.canUndo() );

        Button btnRedo = new Button(composite, SWT.NONE);
        btnRedo.setText("Redo");
        btnRedo.addListener(SWT.Selection, e->{
            dmd.redo();
            dmdRedraw();
        });
        ObserverManager.bind(dmd, e -> btnRedo.setEnabled(e), ()->dmd.canRedo() );
        
        Group grpPalettes = new Group(shell, SWT.NONE);
        grpPalettes.setLayout(new GridLayout(7, false));
        GridData gd_grpPalettes = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
        gd_grpPalettes.heightHint = 90;
        grpPalettes.setLayoutData(gd_grpPalettes);
        grpPalettes.setText("Palettes");

        paletteComboViewer = new ComboViewer(grpPalettes, SWT.NONE);
        Combo combo = paletteComboViewer.getCombo();
        combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        paletteComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        paletteComboViewer.setLabelProvider(new LabelProviderAdapter(o->((Palette)o).index + " - " + ((Palette)o).name));
        paletteComboViewer.setInput(project.palettes);
        
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
        paletteTypeComboViewer.addSelectionChangedListener(e->paletteTypeChanged(e));

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
            paletteComboViewer.refresh();
            paletteComboViewer.setSelection(new StructuredSelection(activePalette), true);
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

        Label lblPlanes1 = new Label(grpPalettes, SWT.NONE);
        lblPlanes1.setText("Planes");

        planesComboViewer = new ComboViewer(grpPalettes, SWT.NONE);
        Combo planes = planesComboViewer.getCombo();
        GridData gd_planes = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_planes.widthHint = 60;
        planes.setLayoutData(gd_planes);
        planesComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        planesComboViewer.setInput(PlaneNumber.values());
        planesComboViewer.addSelectionChangedListener(e -> {
        	planeNumer = (PlaneNumber) ((IStructuredSelection) e.getSelection()).getFirstElement();
        	paletteTool.setNumberOfPlanes(planeNumer.numberOfPlanes);
        });

        paletteTool = new PaletteTool(shell, grpPalettes, SWT.FLAT | SWT.RIGHT, activePalette);
        
        drawTools.put("pencil", new SetPixelTool(paletteTool.getSelectedColor()));       
        drawTools.put("fill", new FloodFillTool(paletteTool.getSelectedColor()));       
        drawTools.put("rect", new RectTool(paletteTool.getSelectedColor()));
        drawTools.put("line", new LineTool(paletteTool.getSelectedColor()));
        drawTools.put("circle", new CircleTool(paletteTool.getSelectedColor()));
        drawTools.values().forEach(d->paletteTool.addListener(d));
        
        paletteTool.addListener(dmdWidget);
        
        btnChangeColor = new Button(grpPalettes, SWT.NONE);
        btnChangeColor.setText("Color");
		btnChangeColor.addListener(SWT.Selection, e -> paletteTool.changeColor() );

        drawToolBar = new ToolBar(grpPalettes, SWT.FLAT | SWT.RIGHT);
        drawToolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
        
        ToolItem tltmPen = new ToolItem(drawToolBar, SWT.RADIO);
        tltmPen.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/pencil.png")));
        tltmPen.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("pencil")));
        
        ToolItem tltmFill = new ToolItem(drawToolBar, SWT.RADIO);
        tltmFill.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/color-fill.png")));
        tltmFill.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("fill")));
        
        ToolItem tltmRect = new ToolItem(drawToolBar, SWT.RADIO);
        tltmRect.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/rect.png")));
        tltmRect.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("rect")));
        
        ToolItem tltmLine = new ToolItem(drawToolBar, SWT.RADIO);
        tltmLine.setImage(resManager.createImage(ImageDescriptor.createFromFile(PinDmdEditor.class, "/icons/line.png")));
        tltmLine.addListener(SWT.Selection, e->dmdWidget.setDrawTool(drawTools.get("line")));

        ToolItem tltmCircle = new ToolItem(drawToolBar, SWT.RADIO);
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

    }

	private void sortAnimations() {
		ArrayList<Entry<String,Animation>> list = new ArrayList<>( animations.entrySet() );
		Collections.sort(list, new Comparator<Entry<String,Animation>>() {

			@Override
			public int compare(Entry<String, Animation> o1,
					Entry<String, Animation> o2) {
				return o1.getValue().getDesc().compareTo(o2.getValue().getDesc());
			}
		});
		animations.clear();
		for (Entry<String, Animation> entry : list) {
			animations.put(entry.getKey(), entry.getValue());
		}
	}

	private void dmdRedraw() {
		dmdWidget.redraw();
		previewDmd.redraw();
	}


	void keyFrameChanged(SelectionChangedEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		if (selection.size() > 0) {
			// set new mapping
			selectedPalMapping = (PalMapping) selection.getFirstElement();

			LOG.debug("selected new palMapping {}", selectedPalMapping);

			selectedHashIndex = selectedPalMapping.hashIndex;

			txtDuration.setText(selectedPalMapping.durationInMillis + "");
			paletteComboViewer.setSelection(new StructuredSelection(
					project.palettes.get(selectedPalMapping.palIndex)));
			for (int j = 0; j < numberOfHashes; j++) {
				btnHash[j].setSelection(j == selectedHashIndex);
			}
			selectedAnimation = Optional.of(animations
					.get(selectedPalMapping.animationName));
			aniListViewer.setSelection(new StructuredSelection(
					selectedAnimation.get()));

			animationHandler.setPos(selectedPalMapping.frameIndex);
			saveTimeCode = (int) selectedAnimation.get().getTimeCode(
					selectedPalMapping.frameIndex);
		} else {
			selectedPalMapping = null;
		}
		btnDeleteKeyframe.setEnabled(selection.size() > 0);
		btnFetchDuration.setEnabled(selection.size() > 0);
	}

	void paletteTypeChanged(SelectionChangedEvent e) {
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
    }

	/**
	 * check if dirty.
	 * @return true, if not dirty or if user decides to ignore dirtyness (or global ignore flag is set via cmdline)
	 */
	boolean dirtyCheck() {
		if( project.dirty && !nodirty ) {
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
		
		MenuItem mntmImportProject = new MenuItem(menu_1, SWT.NONE);
		mntmImportProject.setText("Import Project");
		mntmImportProject.addListener(SWT.Selection, e->importProject());
		
		MenuItem mntmExportProject = new MenuItem(menu_1, SWT.NONE);
		mntmExportProject.setText("Export Project");
		mntmExportProject.addListener(SWT.Selection, e->exportProject());
		
		new MenuItem(menu_1, SWT.SEPARATOR);
		
		MenuItem mntmExit = new MenuItem(menu_1, SWT.NONE);
		mntmExit.setText("Exit");
		mntmExit.addListener(SWT.Selection, e->{
			if( dirtyCheck() ) {
				shell.close();
				shell.dispose();
			}
		});
		
		MenuItem mntmedit = new MenuItem(menu, SWT.CASCADE);
		mntmedit.setText("&Edit");
		
		Menu menu_5 = new Menu(mntmedit);
		mntmedit.setMenu(menu_5);
		
		MenuItem mntmUndo = new MenuItem(menu_5, SWT.NONE);
		mntmUndo.setText("Undo");
		mntmUndo.addListener(SWT.Selection, e-> {
			dmd.undo();
			dmdRedraw();
		});
		ObserverManager.bind(dmd, e->mntmUndo.setEnabled(e), () -> dmd.canUndo());
		
		MenuItem mntmRedo = new MenuItem(menu_5, SWT.NONE);
		mntmRedo.setText("Redo");
		mntmRedo.addListener(SWT.Selection, e->{
			dmd.redo();
			dmdRedraw();
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
		
		new MenuItem(menu_2, SWT.SEPARATOR);
		
		MenuItem mntmSaveAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmSaveAnimation.setText("Save Animation");
		mntmSaveAnimation.addListener(SWT.Selection, e->saveAniWithFC() );

		MenuItem mntmExportAnimation = new MenuItem(menu_2, SWT.NONE);
		mntmExportAnimation.setText("Export Animation as GIF");
		mntmExportAnimation.addListener(SWT.Selection, e-> {
			GifExporter exporter = new GifExporter(shell, activePalette, playingAnis.get(0));
			exporter.open();	
		});
		
		MenuItem mntmpalettes = new MenuItem(menu, SWT.CASCADE);
		mntmpalettes.setText("&Palettes / Mode");
		Menu menu_3 = new Menu(mntmpalettes);
		mntmpalettes.setMenu(menu_3);
		
		MenuItem mntmLoadPalette = new MenuItem(menu_3, SWT.NONE);
		mntmLoadPalette.setText("Load Palette");
		mntmLoadPalette.addListener(SWT.Selection, e->loadPalette());
		
		MenuItem mntmSavePalette = new MenuItem(menu_3, SWT.NONE);
		mntmSavePalette.setText("Save Palette");
		mntmSavePalette.addListener(SWT.Selection, e->savePalette());
		
        new MenuItem(menu_3, SWT.SEPARATOR);

        MenuItem mntmDevice = new MenuItem(menu_3, SWT.NONE);
        mntmDevice.setText("Configure Device");
        mntmDevice.addListener(SWT.Selection, e->new DeviceConfig(shell).open());
        
        MenuItem mntmhelp = new MenuItem(menu, SWT.CASCADE);
		mntmhelp.setText("&Help");
		
		Menu menu_4 = new Menu(mntmhelp);
		mntmhelp.setMenu(menu_4);
		
		MenuItem mntmGetHelp = new MenuItem(menu_4, SWT.NONE);
		mntmGetHelp.setText("Get help");
		mntmGetHelp.addListener(SWT.Selection, e->Program.launch(HELP_URL));
		
		MenuItem mntmRegister = new MenuItem(menu_4, SWT.NONE);
		mntmRegister.setText("Register");
		mntmRegister.addListener(SWT.Selection, e->new RegisterLicense(shell).open());
		
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


    @Override
    public void notifyAni(AniEvent evt) {
        switch (evt.evtType) {
        case ANI:
            lblFrameNo.setText(""+ evt.actFrame);
            lblTcval.setText( ""+evt.timecode);
            lblDelayVal.setText(""+evt.delay);
            lblPlanesVal.setText(""+evt.nPlanes);
            //hashLabel.setText(
            int i = 0;
            for( byte[] p : evt.hashes) {
            	String hash = getPrintableHashes(p);
            	if( hash.startsWith("B2AA7578" /*"BF619EAC0CDF3F68D496EA9344137E8B" */ )) { // disable for empty frame
            		btnHash[i].setText("");
            		btnHash[i].setEnabled(false);
            	} else {
            		btnHash[i].setText(hash);
            		btnHash[i].setEnabled(true);
            	}
            	i++;
            	if( i>= btnHash.length) break;
			}
            while(i<4) {
        		btnHash[i].setText("");
        		btnHash[i].setEnabled(false);
        		i++;
            }

            saveHashes(evt.hashes);
            lastTimeCode = evt.timecode;
            break;
        case CLOCK:
            lblFrameNo.setText("");
            lblTcval.setText("");
            // sourceList.deselectAll();
            for(int j=0; j<4; j++) btnHash[j++].setText(""); // clear hashes
            break;
        case CLEAR:
        	for(int j=0; j<4; j++) btnHash[j++].setText(""); // clear hashes
        	break;
        }
        dmdRedraw();
    }
}
