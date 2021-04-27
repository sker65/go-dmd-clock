package com.rinke.solutions.pinball;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.BeanFactory;
import com.rinke.solutions.beans.SimpleBeanFactory;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.api.LicenseManagerFactory;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.view.ChangeHandlerHelper;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.handler.AnimationControlHandler;
import com.rinke.solutions.pinball.view.handler.AutosaveHandler;
import com.rinke.solutions.pinball.view.handler.CommandHandler;
import com.rinke.solutions.pinball.view.handler.CutCmdHandler;
import com.rinke.solutions.pinball.view.handler.DrawCmdHandler;
import com.rinke.solutions.pinball.view.handler.MenuHandler;
import com.rinke.solutions.pinball.view.handler.ProjectHandler;
import com.rinke.solutions.pinball.view.handler.ViewBindingHandler;
import com.rinke.solutions.pinball.view.model.ViewModel;

//@Slf4j
public class PinDmdEditor {

	@Option(name = "-ani", usage = "animation file to load", required = false)
	private String aniToLoad;

	@Option(name = "-play", usage = "animation file to play fullscreen", required = false)
	private String playFile;

	@Option(name = "-cut", usage = "<src name>,<new name>,<start>,<end>", required = false)
	private String cutCmd;

	@Option(name = "-nodirty", usage = "dont check dirty flag on close", required = false)
	private boolean nodirty = false;

	@Option(name = "-save", usage = "if set, project is saved right away", required = false)
	private String saveFile;

	@Option(name = "-load", usage = "if set, project is loaded right away", required = false)
	private String loadFile;

	@Argument
	private java.util.List<String> arguments = new ArrayList<String>();

	int numberOfHashes = 4;

	private String pluginsPath;
	private List<String> loadedPlugins = new ArrayList<>();

	ChangeHandlerHelper<ViewBindingHandler> viewModelChangeHandler;
	BeanFactory beanFactory;

	@Autowired private AnimationActionHandler aniAction;
	@Autowired private AutosaveHandler autoSaveHandler;
	@Autowired private ViewModel vm;
	@Autowired private CutCmdHandler cutCmdHandler;
	@Autowired private DrawCmdHandler drawCmdHandler;
	@Autowired private AnimationControlHandler animationControlHandler;
	@Autowired private ProjectHandler projectHandler;
	@Autowired public AnimationHandler animationHandler;
	@Autowired private CmdDispatcher dispatcher;
	@Autowired private MenuHandler menuHandler;
	
	int actFrameOfSelectedAni = 0;

	@Value String pin2dmdAdress;

	MainView mainView;

	public PinDmdEditor() {
		// avoid NPE we run in test context
		if( log == null ) {
			log = LoggerFactory.getLogger(PinDmdEditor.class);
		}
		checkForPlugins();

		mainView = new EditorView(numberOfHashes, !nodirty);
		beanFactory = new SimpleBeanFactory();
	}
	
	static File logFile;
	static Logger log;
	
	/**
	 * create a tmp file that will be deleted on jvm exit and redirect log output to that file
	 * this enables the 'show log lines' feature in the global crash dialog.
	 */
	public static void configureLogging()
	{
		try {
			logFile = File.createTempFile("pin2dmd-editor", ".log");
			logFile.deleteOnExit();
		} catch (IOException e) {
			System.err.println("problems with creating logfile ...");
			e.printStackTrace();
		}
		if( System.getProperty(SimpleLogger.LOG_FILE_KEY) == null ) {
			System.setProperty(SimpleLogger.LOG_FILE_KEY, logFile.getAbsolutePath());
			System.out.println("logging to "+logFile.getAbsolutePath());
		}			
	}

	void checkForPlugins() {
		Path currentRelativePath = Paths.get("");
		pluginsPath = currentRelativePath.toAbsolutePath().toString()+File.separator+"plugins";
		String[] fileList = new File(pluginsPath).list((dir, name) -> name.endsWith(".jar"));
		if( fileList!=null) Arrays.stream(fileList).forEach(file -> addSoftwareLibrary(new File(pluginsPath+File.separatorChar+file)));
		try {
			Class.forName("org.bytedeco.javacv.Java2DFrameConverter");
			log.info("successfully loaded video plugin classes");
			loadedPlugins.add("Video");
		} catch (ClassNotFoundException e) {
		}
	}
	
	private  void addSoftwareLibrary(File file) {
		try {
		    Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
		    method.setAccessible(true);
		    method.invoke(ClassLoader.getSystemClassLoader(), new Object[]{file.toURI().toURL()});
		    log.info("adding {} to classpath", file.toURI().toURL());
		} catch( Exception e) {
			log.warn("adding {} to classpath failed", file.getPath());
		}
	}
	
	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		configureLogging();
		log = LoggerFactory.getLogger(PinDmdEditor.class);

		Loader.load(opencv_java.class);
		
		PinDmdEditor editor = new PinDmdEditor();
		editor.parseCmdLine(args);
		editor.open(args);
		
		log.info("exiting");
	}

	public void open(String[] args) {
		
		Config config = new Config();
		config.load();
		beanFactory.setValueProvider(config);
		beanFactory.scanPackages("com.rinke.solutions.pinball");
		
		beanFactory.setSingleton("beanFactory", beanFactory);
		beanFactory.setSingleton("pluginsPath", pluginsPath);
		beanFactory.setSingleton("plugins", loadedPlugins );
		beanFactory.setSingleton("license", LicenseManagerFactory.getInstance());

		DmdSize ds = DmdSize.fromOrdinal(beanFactory.getBeanByType(Config.class).getInteger(Config.DMDSIZE,0));
		DMD dmd = new DMD(ds.width, ds.height);
		beanFactory.setSingleton("dmd", dmd );
		
		vm = beanFactory.getBeanByType(ViewModel.class);
		vm.init(dmd, ds, pin2dmdAdress, 24);
		
		mainView.init(vm, beanFactory);

		beanFactory.inject(this);
		beanFactory.inject(mainView);
		for(Object target: mainView.getInjectTargets())
			beanFactory.inject(target);
		
		autoSaveHandler.setMainView(mainView);

		mainView.createBindings();
		
		menuHandler.onNewProject();
		autoSaveHandler.checkAutoSaveAtStartup();
		animationHandler.setEventHandler(drawCmdHandler);
		
		// register all view handlers with the dispatcher
		List<CommandHandler> handlers = beanFactory.getBeansOfType(CommandHandler.class);
		handlers.forEach(h->dispatcher.registerHandler(h));
		
		//dispatcher.checkChangeHandlers(vm);
		
		viewModelChangeHandler = new ChangeHandlerHelper<ViewBindingHandler>(beanFactory, ViewBindingHandler.class);
		
		vm.addPropertyChangeListener( e->viewModelChanged(e) );
		
		// process cmdLine AFTER all handler are registered
		processCmdLine();
		
		vm.setMinFrame(0);
		vm.setMaxFrame(0);
		
		mainView.open();
		
	}

	void parseCmdLine(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();
			System.exit(1);
		}
	}

	void processCmdLine() {
		// cmd line processing
		if (loadFile != null) {
			projectHandler.onLoadProjectWithProgress(loadFile, null);
			vm.setProjectFilename(loadFile);
		}
		if (aniToLoad != null) {
			aniAction.loadAni(aniToLoad, false, true, null);
		}
		if (playFile != null) {
			List<Animation> anis = aniAction.loadAni(playFile, false, true, null);
			vm.playingAnis.addAll(anis);
			// start playback
			animationControlHandler.onStartStop(true);
			mainView.playFullScreen();
		}
		if (cutCmd != null && !vm.recordings.isEmpty()) {
			String[] cuts = cutCmd.split(",");
			if (cuts.length >= 3) {
				cutCmdHandler.cutScene(vm.recordings.get(cuts[0]), Integer.parseInt(cuts[2]), Integer.parseInt(cuts[3]), cuts[1]);
			}
		}
		if (saveFile != null) {
			projectHandler.saveProject(saveFile, true);
		}
	}
	
	void viewModelChanged(PropertyChangeEvent e) {
		String propName = e.getPropertyName();
		Object nv = e.getNewValue();
		Object ov = e.getOldValue();
		if( nv == null && ov == null ) return;
		log.debug("view model changed {} {}->{}", e.getPropertyName(), e.getOldValue(), e.getNewValue());

		List<ViewBindingHandler> handlers = beanFactory.getBeansOfType(ViewBindingHandler.class);
		handlers.stream().forEach(h->h.viewModelChanged(propName, ov, nv));
		
		viewModelChangeHandler.callOnChangedHandlers(propName, nv, ov);
	}


	

}
