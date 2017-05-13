package com.rinke.solutions.pinball.view.handler;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.AniActionHandler;
import com.rinke.solutions.pinball.AnimationActionHandler;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.TypedLabel;
import com.rinke.solutions.pinball.view.model.ViewModel;
import com.rinke.solutions.pinball.view.swt.RecentMenuManager;

@Bean
@Slf4j
public class ProjectHandler extends ViewHandler {

	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	FileChooserUtil fileChooserUtil;
	@Autowired
	FileHelper fileHelper;
	@Autowired
	RecentMenuManager recentProjectsMenuManager;
	@Autowired
	AniActionHandler aniActionHandler;
	
	public ProjectHandler(ViewModel vm, Model model, CmdDispatcher d) {
		super(vm, model, d);
	}
	
	public Project buildProjectFrom(Model m, int version) {
		Project project = new Project();
		project.setDimension(m.dmdSize.width, m.dmdSize.height);
		project.bookmarksMap = model.bookmarksMap;
		project.inputFiles = m.inputFiles;
		project.masks = m.masks;
		project.palettes = m.palettes;
		project.palMappings = m.palMappings;
		project.version = (byte) version;
		return project;
	}
	
	// project handler
	public void onImportProject() {
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.xml;*.json;" }, new String[] { "Project XML", "Project JSON" });
		if (filename != null)
			importProject(filename);
	}
	
	public void importProject(String filename) {
		log.info("importing project from {}", filename);
		Project projectToImport = (Project) fileHelper.loadObject(filename);
		// merge into existing Project
		HashSet<String> collisions = new HashSet<>();
		/* TODO for (String key : projectToImport.frameSeqMap.keySet()) {
			if (project.frameSeqMap.containsKey(key)) {
				collisions.add(key);
			} else {
				project.frameSeqMap.put(key, projectToImport.frameSeqMap.get(key));
			}
		}*/
		if (!collisions.isEmpty()) {
			messageUtil.warn(SWT.ICON_WARNING | SWT.OK | SWT.IGNORE | SWT.ABORT, 
					"Override warning", "the following frame seq have NOT been \nimported due to name collisions: " + collisions + "\n");
		}

		for (String inputFile : projectToImport.inputFiles) {
			aniActionHandler.loadAni(FileChooserUtil.buildRelFilename(filename, inputFile), true, true);
		}
		for (PalMapping palMapping : projectToImport.palMappings) {
			model.palMappings.add(palMapping);
		}
	}
	
	public void onSaveProject(boolean saveAs) {
		if( saveAs || model.filename==null ) {
			String filename = fileChooserUtil.choose(SWT.SAVE, model.filename, new String[] { "*.xml" }, new String[] { "Project XML" });
			if (filename != null)
				saveProject(filename);
		} else {
			saveProject(model.filename);
		}
	}
	
	private void saveProject(String filename) {
		log.info("write project to {}", filename);
		String aniFilename = FileChooserUtil.replaceExtensionTo("ani", filename);
		String baseName = new File(aniFilename).getName();
		String baseNameWithoutExtension = baseName.substring(0, baseName.indexOf('.'));
		if (model.name == null) {
			model.name = baseNameWithoutExtension;
		} else if (!model.name.equals(baseNameWithoutExtension)) {
			// save as
			model.inputFiles.remove(model.name + ".ani");
		}
		
		// we need to "tag" the projects animations that are always stored in the projects ani file
		// the project ani file is not included in the inputFile list but animations gets loaded
		// implicitly
		
		String path = new File(filename).getParent(); 
		// so first check directly included anis in project inputfiles
		for( String inFile : model.inputFiles) {
			Optional<Animation> optAni = model.recordings.values().stream().filter(a -> a.getName().equals(path+File.separator+inFile)).findFirst();
			optAni.ifPresent(a-> {
				if( a.isDirty()) {
					aniActionHandler.storeAnimations(Arrays.asList(a), a.getName(), 4, false);
					a.setDirty(false);
				}
			});
		}
		
		storeOrDeleteProjectAnimations(aniFilename);

		fileHelper.storeObject(buildProjectFrom(model, 2), filename);
		model.dirty = false;

	}
	
	private void storeOrDeleteProjectAnimations(String aniFilename) {
		// only need to save ani's that are 'project' animations
		List<Animation> prjAnis = model.scenes.values().stream().filter(a->a.isProjectAnimation()).collect(Collectors.toList());
		if( !prjAnis.isEmpty() ) {
			aniActionHandler.storeAnimations(prjAnis, aniFilename, 4, true);
		} else {
			new File(aniFilename).delete(); // delete project ani file
		}
	}

	private final String frameTextPrefix = "Pin2dmd Editor ";
	
	public void onLoadProject(String filename) {
		log.info("load project from {}", filename);
		Project loadedProject = (Project) fileHelper.loadObject(filename);

		if (loadedProject != null) {
			vm.setShellTitle(frameTextPrefix + " - " + new File(filename).getName());
			if( loadedProject.width == 0) {
				loadedProject.width = 128;
				loadedProject.height = 32; // default for older projects
			}
			
			model.inputFiles.addAll( loadedProject.inputFiles );
			model.palettes.clear();
			model.palettes.addAll(loadedProject.palettes);
			model.bookmarksMap.clear();
			model.bookmarksMap = loadedProject.bookmarksMap;
			model.palMappings.clear();
			model.palMappings.addAll(loadedProject.palMappings);
			model.masks.clear();
			model.masks.addAll(loadedProject.masks);
			
			model.setDmdSize(DmdSize.fromWidthHeight(loadedProject.width, loadedProject.height));
			model.setFilename(filename);
			
			// if inputFiles contain project filename remove it
			String aniFilename = FileChooserUtil.replaceExtensionTo("ani", filename);
			model.inputFiles.remove(aniFilename); // full name
			model.inputFiles.remove(new File(aniFilename).getName()); // simple name
			
			for (String file : model.inputFiles) {
				aniActionHandler.loadAni(FileChooserUtil.buildRelFilename(filename, file), true, false);
			}
			
			List<Animation> loadedWithProject = aniActionHandler.loadAni(aniFilename, true, false);
			loadedWithProject.stream().forEach(a->{a.setProjectAnimation(true);});
			
			// maybe delegate to pal handler 
			// ensureDefault();
			recentProjectsMenuManager.populateRecent(filename);
		}
	}

	public void onLoadProject() {
		model.reset();
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.xml;*.json;" }, new String[] { "Project XML", "Project JSON" });
		if (filename != null) {
			onLoadProject(filename);
		}		
	}
	
	public void onNewProject() {
		vm.setSelectedBookmark(null);
		vm.setSelectedRecording(null);
		vm.setSelectedScene(null);
		vm.setSelectedFrameSeq(null);
		vm.setSelectedKeyFrame(null);
		// delete / reset model
		model.reset();
		// populate to view model
		Palette p = (Palette) vm.palettes.get(0);
		//vm.setSelectedPalette((Palette) );
	}
	
	public void onUploadProject() {
		
	}

}
