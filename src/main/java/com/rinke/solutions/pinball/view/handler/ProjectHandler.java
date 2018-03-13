package com.rinke.solutions.pinball.view.handler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.AnimationActionHandler;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.api.BinaryExporter;
import com.rinke.solutions.pinball.api.BinaryExporterFactory;
import com.rinke.solutions.pinball.api.LicenseManager;
import com.rinke.solutions.pinball.api.LicenseManager.Capability;
import com.rinke.solutions.pinball.io.FileHelper;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameSeq;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class ProjectHandler extends AbstractCommandHandler {

	@Autowired FileChooserUtil fileChooserUtil;
	@Autowired FileHelper fileHelper;
	@Autowired MessageUtil messageUtil;
	@Autowired AnimationActionHandler aniAction;
	@Autowired LicenseManager licenseManager;

	@Value(key=Config.OLDEXPORT)
	boolean useOldExport;
	
	@Value
	boolean backup;

	
	public ProjectHandler(ViewModel vm) {
		super(vm);
	}

	public void onLoadProject() {
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.xml;*.json;" }, new String[] { "Project XML", "Project JSON" });
		if (filename != null) {
			onLoadProject(filename);
		}
	}

	/**
	 * imports a secondary project to implement a merge functionality
	 */
	public void onImportProject() {
		String filename = fileChooserUtil.choose(SWT.OPEN, null, new String[] { "*.xml;*.json;" }, new String[] { "Project XML", "Project JSON" });
		if (filename != null)
			importProject(filename);
	}

	void importProject(String filename) {
		log.info("importing project from {}", filename);
		Project projectToImport = (Project) fileHelper.loadObject(filename);
		
		// not needed anymore project.populatePaletteToMap();
		
		// merge into existing Project
		HashSet<String> collisions = new HashSet<>();
		/*for (String key : projectToImport.frameSeqMap.keySet()) {
			if (project.frameSeqMap.containsKey(key)) {
				collisions.add(key);
			} else {
				project.frameSeqMap.put(key, projectToImport.frameSeqMap.get(key));
			}
		}*/
		if (!collisions.isEmpty()) {
			messageUtil.warn(SWT.ICON_WARNING | SWT.OK | SWT.IGNORE | SWT.ABORT,
					"Override warning",
					"the following frame seq have NOT been \nimported due to name collisions: " + collisions + "\n");
		}

		for (String inputFile : projectToImport.inputFiles) {
			aniAction.loadAni(buildRelFilename(filename, inputFile), true, true);
		}
		for (PalMapping palMapping : projectToImport.palMappings) {
			vm.keyframes.put(palMapping.name,palMapping);
		}
	}
	
	String replaceExtensionTo(String newExt, String filename) {
		int p = filename.lastIndexOf(".");
		if (p != -1)
			return filename.substring(0, p) + "." + newExt;
		return filename;
	}

	public void onLoadProject(String filename) {
		log.info("load project from {}", filename);
		Project projectToLoad = (Project) fileHelper.loadObject(filename);
		if (projectToLoad != null) {
			//projectToLoad.populatePaletteToMap();
			vm.populatePaletteToMap(projectToLoad.getPalettes());
			if( projectToLoad.width == 0) {
				projectToLoad.width = 128;
				projectToLoad.height = 32; // default for older projects
			}
			DmdSize newSize = DmdSize.fromWidthHeight(projectToLoad.width, projectToLoad.height);
			vm.setDmdSize(newSize);
			vm.setProjectFilename(filename);
			Project project = projectToLoad;
			vm.recordings.clear();
			vm.scenes.clear();
			
			// if inputFiles contain project filename remove it
			String aniFilename = replaceExtensionTo("ani", filename);
			project.inputFiles.remove(aniFilename); // full name
			project.inputFiles.remove(new File(aniFilename).getName()); // simple name
			String msg = "";
			for (String file : project.inputFiles) {
				try {
					List<Animation> anis = aniAction.loadAni(buildRelFilename(filename, file), true, false);
					if( !anis.isEmpty() ) {
						Animation firstAni = anis.get(0);
						if( project.recordingNameMap.containsKey(file)) {
							firstAni.setDesc(project.recordingNameMap.get(file));
						}
					}
				} catch( RuntimeException e) {
					msg +="\nProblem loading "+file+": "+e.getMessage();
				}
			}
			
			List<Animation> loadedWithProject = aniAction.loadAni(aniFilename, true, false);
			loadedWithProject.stream().forEach(a->a.setProjectAnimation(true));
			
			// populate palettes
			vm.populatePaletteToMap(project.getPalettes());
			vm.setSelectedPalette(firstFromMap(vm.paletteMap));
			
			// populate keyframes
			vm.keyframes.clear();
			project.palMappings.stream().forEach(pm->vm.keyframes.put(pm.name, pm));
			
			vm.setDirty(false);
			
			//setupUIonProjectLoad();
			
			ensureDefault();
			vm.setRecentProjects(filename);
			if( !StringUtils.isEmpty(msg)) {
				messageUtil.warn("Not all files loaded", msg);
			}
		}

	}

	private <T> T firstFromMap(Map<?, T> map) {
		return map.size() > 0 ? map.values().iterator().next() : null;
	}

	private void ensureDefault() {
		boolean foundDefault = false;
		for (Palette p : vm.paletteMap.values()) {
			if (PaletteType.DEFAULT.equals(p.type)) {
				foundDefault = true;
				break;
			}
		}
		if (!foundDefault) {
			vm.paletteMap.get(0).type = PaletteType.DEFAULT;
		}
	}

	String buildRelFilename(String parent, String file) {
		if( file.contains(File.separator)) return file;
		return new File(parent).getParent() + File.separator + new File(file).getName();
	}

	void onExportRealPinProject() {
		licenseManager.requireOneOf( Capability.REALPIN, Capability.GODMD, Capability.XXL_DISPLAY);
		String filename = fileChooserUtil.choose(SWT.SAVE, vm.projectFilename, new String[] { "*.pal" }, new String[] { "Export pal" });
		if (filename != null) {
			messageUtil.warn("Warning", "Please don´t publish projects with copyrighted material / frames");
			onExportProject(filename, f -> new FileOutputStream(f), true);
			if( !filename.endsWith("pin2dmd.pal")) {
				messageUtil.warn("Hint", "Remember to rename your export file to pin2dmd.pal if you want to use it" + " in a real pinballs sdcard of pin2dmd.");
			}
		}
	}
	
	void onExportVirtualPinProject() {
//		licManager.requireOneOf(Capability.VPIN, Capability.GODMD);
		String filename = fileChooserUtil.choose(SWT.SAVE, vm.projectFilename, new String[] { "*.pal" }, new String[] { "Export pal" });
		if (filename != null) {
			messageUtil.warn("Warning", "Please don´t publish projects with copyrighted material / frames");
			onExportProject(filename, f -> new FileOutputStream(f), false);
		}
	}

	public void onSaveProject(boolean saveAs) {
		if( saveAs || vm.projectFilename==null ) {
			String filename = fileChooserUtil.choose(SWT.SAVE, vm.projectFilename, new String[] { "*.xml" }, new String[] { "Project XML" });
			if (filename != null) {
				onSaveProject(filename);
				vm.setProjectFilename(filename);
			}
		} else {
			onSaveProject(vm.projectFilename);
		}
	}

	@FunctionalInterface
	public interface OutputStreamProvider {
		OutputStream buildStream(String name) throws IOException;
	}

	public void onExportProject(String filename, OutputStreamProvider streamProvider, boolean realPin) {
		log.info("export project {} file {}", realPin?"real":"vpin", filename);
		if( realPin) licenseManager.requireOneOf(Capability.VPIN, Capability.REALPIN, Capability.GODMD, Capability.XXL_DISPLAY);

		Project project = new Project();
		// populate everything
		
		// rebuild frame seq map	
		HashMap <String,FrameSeq> frameSeqMap = new HashMap<>();
		Iterator<PalMapping> it = vm.keyframes.values().iterator();
		while( it.hasNext()) {
			PalMapping p = it.next();
			if (p.frameSeqName != null ) {
				if( vm.scenes.containsKey(p.frameSeqName) ) {
					FrameSeq frameSeq = new FrameSeq(p.frameSeqName);
					if (p.switchMode.masking ) {
						frameSeq.mask = 0b11111100;
					}
					frameSeqMap.put(p.frameSeqName, frameSeq);
				} else {
					log.error("referenced scene not found, keyframe will be removed: {}", p);
					it.remove();
				}
			}
		}
		
		// VPIN
		if( !realPin ) {
			List<Animation> anis = new ArrayList<>();
			for (FrameSeq p : frameSeqMap.values()) {
				Animation ani = vm.scenes.get(p.name);
				// copy without extending frames
				CompiledAnimation cani = ani.cutScene(ani.start, ani.end, 0);
				cani.actFrame = 0;
				cani.setDesc(ani.getDesc());
				DMD tmp = new DMD(vm.dmdSize.width, vm.dmdSize.height);
				for (int i = cani.start; i <= cani.end; i++) {
					cani.getCurrentMask();
					Frame f = cani.render(tmp, false);
					for( int j = 0; j < f.planes.size(); j++) {
						if (((1 << j) & p.mask) == 0) {
							// dont remove original frames form vni format
							// Arrays.fill(f.planes.get(j).data, (byte)0);
						}
					}
				}
				anis.add(cani);
			}
			if( !anis.isEmpty() ) {
				String aniFilename = replaceExtensionTo("vni", filename);
				AniWriter aniWriter = new AniWriter(anis, aniFilename, 4, vm.paletteMap, null);
				aniWriter.setHeader("VPIN");
				aniWriter.run();
				try {
					BinaryExporter exporter = BinaryExporterFactory.getInstance();
					DataOutputStream dos2 = new DataOutputStream(streamProvider.buildStream(filename));
					// for vpins version is 2
					project.version = 2;
					exporter.writeTo(dos2, aniWriter.getOffsetMap(), project);
					dos2.close();
				} catch (IOException e) {
					throw new RuntimeException("error writing " + filename, e);
				}
				/*XStream xStream = new XStream();
				try {
					xStream.toXML(anis, new FileWriter("/Users/stefanri/ani.aml"));
				} catch (IOException e) {
					e.printStackTrace();
				}*/
			}
			
		} else {
			// for all referenced frame mapping we must also copy the frame data as
			// there are two models
			for (FrameSeq p : frameSeqMap.values()) {
				CompiledAnimation ani = vm.scenes.get(p.name);			
				ani.actFrame = 0;
				DMD tmp = new DMD(vm.dmdSize.width, vm.dmdSize.height);
				for (int i = 0; i <= ani.end; i++) {
					ani.getCurrentMask();
					Frame frame = new Frame( ani.render(tmp, false) ); // copy frames to not remove in org
					// remove planes not in mask
					int pl = 0;
					for (Iterator<Plane> iter = frame.planes.iterator(); iter.hasNext();) {
						iter.next();
						if (((1 << pl) & p.mask) == 0) {
							iter.remove();
						}
						pl++;
					}
					p.frames.add(frame);
				}
			}
			// create addtional files for frame sequences
			try {
				Map<String, Integer> map = new HashMap<String, Integer>();
				BinaryExporter exporter = BinaryExporterFactory.getInstance();
				if (!frameSeqMap.isEmpty()) {
					log.info("exporter instance {} wrinting FSQ", exporter);
					DataOutputStream dos = new DataOutputStream(streamProvider.buildStream(replaceExtensionTo("fsq", filename)));
					map = exporter.writeFrameSeqTo(dos, frameSeqMap, useOldExport?1:2);
					dos.close();
				}

				project.version = 1;
				DataOutputStream dos2 = new DataOutputStream(streamProvider.buildStream(filename));
				exporter.writeTo(dos2, map, project);
				dos2.close();
				// fileHelper.storeObject(project, filename);
			} catch (IOException e) {
				throw new RuntimeException("error writing " + filename, e);
			}
		}		
	}

	public void onSaveProject(String filename) {
		log.info("write project to {}", filename);
		String aniFilename = replaceExtensionTo("ani", filename);
		
		if( backup ) {
			backupFiles(filename, aniFilename);
		}
		Project project = new Project();
		// TODO populate everything
		
		String baseName = new File(aniFilename).getName();
		String baseNameWithoutExtension = baseName.substring(0, baseName.indexOf('.'));
		if (project.name == null) {
			project.name = baseNameWithoutExtension;
		} else if (!project.name.equals(baseNameWithoutExtension)) {
			// save as
			project.inputFiles.remove(project.name + ".ani");
		}
		project.setDimension(vm.dmdSize.width, vm.dmdSize.height);
		
		// we need to "tag" the projects animations that are always stored in the projects ani file
		// the project ani file is not included in the inputFile list but animations gets loaded
		// implicitly
		
		String path = new File(filename).getParent(); 
		// so first check directly included anis in project inputfiles
		for( String inFile : project.inputFiles) {
			Optional<Animation> optAni = vm.recordings.values().stream().filter(a -> a.getName().equals(path+File.separator+inFile)).findFirst();
			optAni.ifPresent(a-> {
				if( a.isDirty()) {
					aniAction.storeAnimations(Arrays.asList(a), a.getName(), 4, false);
					a.setDirty(false);
				}
			});
		}
		
		storeOrDeleteProjectAnimations(aniFilename);
		fileHelper.storeObject(project, filename);
		
		vm.setDirty(false);
	}

	private void backupFiles(String... filenames) {
		for(String file: filenames) {
			log.info("creating backup of file '{}'", file);
			try {
				Files.copy( Paths.get(file), Paths.get(file+".bak"));
			} catch (IOException e) {
				log.warn("backup of {} failed", file, e);
			}
		}
	}

	private void storeOrDeleteProjectAnimations(String aniFilename) {
		// only need to save ani's that are 'project' animations
		List<Animation> prjAnis = vm.scenes.values().stream().filter(a->a.isProjectAnimation()).collect(Collectors.toList());
		if( !prjAnis.isEmpty() ) {
			aniAction.storeAnimations(prjAnis, aniFilename, 4, true);
		} else {
			new File(aniFilename).delete(); // delete project ani file
		}
	}



}
