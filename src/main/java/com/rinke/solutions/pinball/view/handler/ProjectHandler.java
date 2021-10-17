package com.rinke.solutions.pinball.view.handler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import static java.nio.file.StandardCopyOption.*;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.AnimationActionHandler;
import com.rinke.solutions.pinball.Constants;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.Dispatcher;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.Worker;
import com.rinke.solutions.pinball.animation.AniReader;
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
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.PaletteType;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.ui.IProgress;
import com.rinke.solutions.pinball.ui.Progress;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class ProjectHandler extends AbstractCommandHandler {

	@Autowired FileChooserUtil fileChooserUtil;
	@Autowired FileHelper fileHelper;
	@Autowired MessageUtil messageUtil;
	@Autowired Dispatcher dispatcher;
	@Autowired IProgress progress;
	@Autowired AnimationActionHandler aniAction;
	@Autowired LicenseManager licenseManager;
	@Autowired Config config;

	@Value(key=Config.OLDEXPORT)
	boolean useOldExport;
	
	@Value(key=Config.NO_EXPORT_WARNING)
	boolean noExportWarning;
	
	@Value
	boolean backup;

	public static final int CURRENT_PRJ_ANI_VERSION = 8;

	public ProjectHandler(ViewModel vm) {
		super(vm);
	}
	
	public void onDmdSizeChanged( DmdSize o, DmdSize newSize) {
		vm.dmd.setSize(newSize.width, newSize.height);
		vm.init(vm.dmd, newSize, vm.prjDmdSize, vm.pin2dmdAdress, vm.maxNumberOfMasks, config);
		vm.setDmdDirty(true);
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
			aniAction.loadAni(buildRelFilename(filename, inputFile), true, true, progress);
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
		Worker w = new Worker() {
			@Override
			public void innerRun() {
				onLoadProjectWithProgress(filename, this);
			}
		};
		w.setProgressEvt(progress);
		w.setInterval(100);
		progress.setText("Loading Project");
		progress.open(w);
		if( w.hasError() ) {
			messageUtil.warn(SWT.ICON_ERROR | SWT.OK,
					"Error loading project",
					"the following error occured while loading: " + w.getRuntimeException().getMessage() + "\n");

		}
	}

	private int index = 0;
	
	public void onLoadProjectWithProgress(String filename, Worker w) {
		log.info("load project from {}", filename);
		// TODO clear view model
		if( w!=null) w.notify(10, "loading project file");
		Project p = (Project) fileHelper.loadObject(filename);
		if (p != null) {
			if( p.version < 2 ) {
				int res = messageUtil.warn(0, "Warning",
						"Older project file", 
						"This project was written with an older version of the editor.\nSaving the project"
								+ " will convert it to the new format,\nthat may not work with the older version.",
						new String[]{"", "Cancel", "Proceed"},2);
				if( res != 2 ) return;
			}
			if( p.version > 2 ) {
				int res = messageUtil.warn(0, "Warning",
						"Newer project file", 
						"This project was written with an newer version of the editor.\nSaving the project"
								+ " will may destroy some of the newer data structures.\nSo please dont override project file w/o backup.",
						new String[]{"", "Cancel", "Got it"},2);
				if( res != 2 ) return;
			}
			// changes in the view model need to run in the UI thread
			dispatcher.syncExec( () -> {
				// populate palettes
				vm.paletteMap.clear();
				if (p.paletteMap.get(0).numberOfColors == 16 && vm.numberOfColors == 64) {
					int res = messageUtil.warn(0, "Warning",
							"16 color project file", 
							"This project has 16 color palettes.\nDo you want to "
									+ "convert it to 64 color format,\nthat may not work with the older version of the editor ?",
							new String[]{"", "Continue", "Convert"},2);
					if( res != 2 ) { 
						vm.numberOfColors = 16; 
						vm.noOfPlanesWhenCutting = 4;
						vm.paletteMap.putAll(p.paletteMap);
						for( Palette pal : p.getPalettes() ) {
							if( !vm.paletteMap.containsKey(pal.index) ) vm.paletteMap.put(pal.index, pal);
							else {
								int r = messageUtil.warn(0, "Warning",
										"duplicate palette", "project file contains conflicting palette definition. Pal No "+pal.index+ "\nDo you want to overwrite ?",
										new String[]{"", "KEEP", "OVERWRITE"},2);
								if( r == 2 ) {
									vm.paletteMap.put(pal.index, pal);
								}
							}
						}
					} else {
						vm.numberOfColors = 64; 
						vm.noOfPlanesWhenCutting = 6;
						for (Palette pal : p.paletteMap.values()) {
							int index = pal.index;
							String name = pal.name;
							RGB rgb[] = new RGB[64];
							for( int j = 0; j<16; j++) {
								rgb[j] = pal.colors[j];
								rgb[j+16] = pal.colors[j];
								rgb[j+32] = pal.colors[j];
								rgb[j+48] = pal.colors[j];
							}
							Palette newPal = new Palette(rgb,index,name);
                            if( !vm.paletteMap.containsKey(index) ) vm.paletteMap.put(index, newPal);
                            else {
                                int r = messageUtil.warn(0, "Warning",
                                        "duplicate palette", "project file contains conflicting palette definition. Pal No "+pal.index+ "\nDo you want to overwrite ?",
                                        new String[]{"", "KEEP", "OVERWRITE"},2);
                                if( r == 2 ) {
                                	vm.paletteMap.put(index, newPal);
                                }
                            }
							
						}
					}
				} else {
					vm.paletteMap.putAll(p.paletteMap);
					if (p.paletteMap.get(0).numberOfColors == 64 && p.noOfPlanesWhenCutting == 4)
						vm.noOfPlanesWhenCutting = 6;
					else
						vm.noOfPlanesWhenCutting = p.noOfPlanesWhenCutting;
					for( Palette pal : p.getPalettes() ) {
						if( !vm.paletteMap.containsKey(pal.index) ) vm.paletteMap.put(pal.index, pal);
						else {
							int r = messageUtil.warn(0, "Warning",
									"duplicate palette", "project file contains conflicting palette definition. Pal No "+pal.index+ "\nDo you want to overwrite ?",
									new String[]{"", "KEEP", "OVERWRITE"},2);
							if( r == 2 ) {
								vm.paletteMap.put(pal.index, pal);
							}
						}
					}
				}
				
				if( p.width == 0) {
					p.width = 128;
					p.height = 32; // default for older projects
				}
				p.srcWidth = p.width;
				p.srcHeight = p.height;
				DmdSize newSize = DmdSize.fromWidthHeight(p.width, p.height);
				vm.dmd.setSize(p.width, p.height);
				vm.setDmdSize(newSize);
				vm.setSrcDmdSize(DmdSize.fromWidthHeight(p.srcWidth, p.srcHeight));
				vm.setProjectFilename(filename);
				vm.recordings.clear();
				vm.has4PlanesRecording = false;
				vm.scenes.clear();
				vm.inputFiles.clear();
				for (String file : p.inputFiles) {
					String basefile = FilenameUtils.getBaseName(file)
			                + "." + FilenameUtils.getExtension(file);
					if (Files.exists(Paths.get(buildRelFilename(filename, file)),java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
						if (file.equals(basefile))
							vm.inputFiles.add(basefile);
						else {
							try { // found file but not in project directory => copy
								Files.copy(Paths.get(buildRelFilename(filename, file)),Paths.get(buildRelFilename(filename, basefile)) , REPLACE_EXISTING);
								vm.inputFiles.add(basefile);
							} catch (IOException f) {
								log.error("problem moving {}", basefile, f);
								vm.inputFiles.add(file);
							}
						}
					} else {
						messageUtil.warn("Project file not found", "Project file " + basefile + " missing. Please copy the file to the project folder and reload project.");
					}
				}
				//vm.addAll(p.inputFiles);
				
			});
			// if inputFiles contain project filename remove it
			String aniFilename = replaceExtensionTo("ani", filename);
			p.inputFiles.remove(aniFilename); // full name
			p.inputFiles.remove(new File(aniFilename).getName()); // simple name
			String msg = "";
			int i = 1;
			index = 0;
			for (String file : vm.inputFiles) {
				String basefile = vm.inputFiles.get(index);
				if( w!=null) w.notify(10 + i*(80/p.inputFiles.size()), "loading ani "+file);
				dispatcher.syncExec(()->{
					try { // load from project folder
						List<Animation> anis = aniAction.loadAni(buildRelFilename(filename, basefile), true, false, progress);
						if( !anis.isEmpty() ) {
							Animation firstAni = anis.get(0);
							if( p.recordingNameMap.containsKey(basefile)) {
								firstAni.setDesc(p.recordingNameMap.get(basefile));
							}
						}
					} catch( RuntimeException e) {
						log.error("problem loading {}", vm.inputFiles.get(i), e);
					}
				});
				index++;
			}
			if( w!=null) w.notify(90, "loading project ani "+aniFilename);

			dispatcher.syncExec(()->{
				List<Animation> loadedWithProject = aniAction.loadAni(aniFilename, true, false, progress);
				vm.setLoadedAniVersion(aniAction.getVersionOfLastLoad());
				loadedWithProject.stream().forEach(a->a.setProjectAnimation(true));

				vm.recordingNameMap.putAll(p.recordingNameMap);
				vm.setSelectedPalette(firstFromMap(vm.paletteMap));
				
				// populate keyframes
				vm.keyframes.clear();
				p.palMappings.stream().forEach(pm->{
					String name = getUniqueName( pm.name, vm.keyframes.keySet());
					pm.name = name;
					vm.keyframes.put(name, pm);
				});
				
				vm.bookmarksMap.putAll(p.bookmarksMap);
				vm.setSelectedRecording(vm.recordings.values().iterator().next());
				
				vm.setDirty(false);
				
				ensureDefault();
				vm.setRecentProjects(filename);
			
			});

			if( !StringUtils.isEmpty(msg)) {
				messageUtil.warn("Not all files loaded", msg);
			}
			
			dispatcher.syncExec(()->{
				// mask
				vm.masks.clear();
				// sanitize masks
				for(Mask m : p.masks) {
					if(m.data.length != vm.srcDmdSize.planeSize) {
						m.data = new byte[vm.srcDmdSize.planeSize];
						log.warn("project detection mask changed to {}",vm.srcDmdSize.planeSize );
					}
				}
				vm.masks.addAll(p.masks);
				log.info("loaded {} masks", vm.masks.size());
			});


		}

	}

	String getUniqueName(String name, Collection<String> set) {
		String res = name;
		int i = 1;
		while( set.contains(res)) {
			res = name + "_" + i;
			i++;
		}
		return res;
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
	
	String bareName(String filename) {
		if( filename == null ) filename = "pin2dmd.xml";
		String b = new File(filename).getName();
		int i = b.lastIndexOf('.');
		return i==-1?b:b.substring(0, i);
	}

	public void onExportRealPinProject() {
		if(licenseManager.getLicense() != null) {
			licenseManager.requireOneOf( Capability.REALPIN, Capability.GODMD, Capability.XXL_DISPLAY);
			String filename = fileChooserUtil.choose(SWT.SAVE, bareName(vm.projectFilename), new String[] { "*.pal" }, new String[] { "Export pal" });
			if (filename != null) {
				if(!noExportWarning ) messageUtil.warn("Warning", "Please don´t publish projects with copyrighted material / frames");
				onExportProject(filename, f -> new FileOutputStream(f), true);
				if( !filename.endsWith("pin2dmd.pal")) {
					if(!noExportWarning ) messageUtil.warn("Hint", "Remember to rename your export file to pin2dmd.pal if you want to use it" + " in a real pinballs sdcard of pin2dmd.");
				}
			}
		} else {
			messageUtil.warn("Warning", "Feature only available with valid license file");
		}
	}
	
	public void onExportVirtualPinProject() {
//		licManager.requireOneOf(Capability.VPIN, Capability.GODMD);
		String filename = fileChooserUtil.choose(SWT.SAVE, bareName(vm.projectFilename), new String[] { "*.pal" }, new String[] { "Export pal" });
		if (filename != null) {
			if(!noExportWarning ) messageUtil.warn("Warning", "Please don´t publish projects with copyrighted material / frames");
			onExportProject(filename, f -> new FileOutputStream(f), false);
		}
	}
	
	public void onSaveProject() {
		if( vm.projectFilename != null) {
			saveProject(vm.projectFilename, true);
		} else {
			onSaveAsProject();
		}
	}

	public void onSaveAsProject() {
		String filename = fileChooserUtil.choose(SWT.SAVE, vm.projectFilename, new String[] { "*.xml" }, new String[] { "Project XML" });
		if (filename != null) {
			saveProject(filename, true);
			vm.setProjectFilename(filename);
		}
	}

	@FunctionalInterface
	public interface OutputStreamProvider {
		OutputStream buildStream(String name) throws IOException;
	}

	public void onExportProject(String filename, OutputStreamProvider streamProvider, boolean realPin) {
		log.info("export project {} file {}", realPin?"real":"vpin", filename);
		if( realPin) licenseManager.requireOneOf(Capability.VPIN, Capability.REALPIN, Capability.GODMD, Capability.XXL_DISPLAY);

		if(vm.selectedScene!=null) {
			vm.selectedScene.commitDMDchanges(vm.dmd); 
			vm.setDirty(vm.dirty|vm.selectedScene.isDirty());
		}

		Project project = new Project();
		int size = project.paletteMap.size();
		for (int i = 0; i < size; i++) {
			project.paletteMap.remove(i);
		}
		populateVmToProject(vm, project);
		List<Mask> filteredMasks = project.masks.stream().filter(m->m.locked).collect(Collectors.toList());
		project.masks = filteredMasks;
		
/*		//filter unused palettes from export
		if (vm.keyframes.size() != 0) {
	 		int size = project.paletteMap.size();
			for (int i = 0; i < size; i++) {
				Palette p = project.paletteMap.get(i);
				if( p.type != PaletteType.DEFAULT ) {
					// check if any keyframe is using this
					boolean keyFrameFound = false;
					for( PalMapping pm : vm.keyframes.values()) {
						if( pm.palIndex == p.index ) {
							if( !keyFrameFound ) {
								keyFrameFound = true;
							}
						}
					}
					if(keyFrameFound == false ) {
						project.paletteMap.remove(p.index);
					}
				}
			}
		}
*/
		
		int aniVersionForExport = 4;
		
		// rebuild frame seq map	
		HashMap <String,FrameSeq> frameSeqMap = new HashMap<>();
		Iterator<PalMapping> it = vm.keyframes.values().iterator();
		while( it.hasNext()) {
			PalMapping p = it.next();
			if (p.frameSeqName != null ) {
				if( vm.scenes.containsKey(p.frameSeqName) ) {
					FrameSeq frameSeq = new FrameSeq(p.frameSeqName);
					if (p.switchMode.masking ) {
						frameSeq.mask = 0b11111111111111111111111111111100;
					}
					if (p.switchMode.equals(SwitchMode.LAYEREDCOL) || p.switchMode.equals(SwitchMode.LAYEREDREPLACE) ) { // ref the scene local masks
						// filter out unlocked masks
						frameSeq.masks = vm.scenes.get(p.frameSeqName).getMasks()
								.stream().filter(m->m.locked).collect(Collectors.toList());
						if ( vm.scenes.get(p.frameSeqName).getMasks().size() != 0 && vm.scenes.get(p.frameSeqName).getMask(0).data.length != vm.srcDmdSize.planeSize ) {
							for (int i = 0; i < frameSeq.masks.size(); i++) {
								Mask mask = new Mask(vm.srcDmdSize.planeSize);
								System.arraycopy(frameSeq.masks.get(i).data, 0, mask.data, 0, vm.srcDmdSize.planeSize);
								frameSeq.masks.set(i, mask);
							}
						}
						// due to a bug in the current firmware, it is mandatory to have a mask in any case
						if (frameSeq.masks.size() == 0) {
							frameSeq.masks.add(new Mask(vm.srcDmdSize.planeSize));
							frameSeq.masks.get(0).locked = true;
						}
						
						if (realPin && !useOldExport) {
							
							// create a "crc" mask: put crc bytes (4 bytes) in a mask plane
							
							int noOfFrames = vm.scenes.get(p.frameSeqName).frames.size();
							int crcMask = frameSeq.masks.size() - 1;
							
							int k = 0;
							for (int i = 0; i < noOfFrames; i++) {
								// for "crc masks" it is okay to use the bigger plane size = dmdSize
								if (i % (vm.srcDmdSize.planeSize / 4) == 0) {
									frameSeq.masks.add(new Mask(vm.srcDmdSize.planeSize)); // add a new plane according to number of CRCs
									if (i>0)
										frameSeq.masks.get(crcMask).data = Frame.transform(frameSeq.masks.get(crcMask).data); // revert bit order for CRCs
									crcMask++;
									frameSeq.masks.get(crcMask).locked = true;
									k = 0;
									}
								byte crc[] = {0,0,0,0};
								crc[0] = vm.scenes.get(p.frameSeqName).frames.get(i).crc32[3];
								crc[1] = vm.scenes.get(p.frameSeqName).frames.get(i).crc32[2];
								crc[2] = vm.scenes.get(p.frameSeqName).frames.get(i).crc32[1];
								crc[3] = vm.scenes.get(p.frameSeqName).frames.get(i).crc32[0];
								System.arraycopy(crc, 0, frameSeq.masks.get(crcMask).data, k++*4, 4);
							}
							frameSeq.masks.get(crcMask).data = Frame.transform(frameSeq.masks.get(crcMask).data); // revert bit order for CRCs
							frameSeq.masks.get(crcMask).locked = true;
						}
					}
					if (p.switchMode.equals(SwitchMode.FOLLOW) || p.switchMode.equals(SwitchMode.FOLLOWREPLACE ) ) { // collect CRCs in mask of first frame.
						if (realPin && !useOldExport) {
							int noOfFrames = vm.scenes.get(p.frameSeqName).frames.size();
							int masksize = vm.scenes.get(p.frameSeqName).frames.get(1).getPlane(0).length;
							if (vm.scenes.get(p.frameSeqName).frames.get(1).mask == null) { //create masks if not exist
								for(int frameNo = 0; frameNo < vm.scenes.get(p.frameSeqName).frames.size();frameNo++) {
									vm.scenes.get(p.frameSeqName).frames.get(frameNo).mask = new Mask(masksize);
								}
							}
							int k = 0;
							for (int i = 0; i < noOfFrames; i++) {
								byte crc[] = {0,0,0,0};
								crc[0] = vm.scenes.get(p.frameSeqName).frames.get(i).crc32[3];
								crc[1] = vm.scenes.get(p.frameSeqName).frames.get(i).crc32[2];
								crc[2] = vm.scenes.get(p.frameSeqName).frames.get(i).crc32[1];
								crc[3] = vm.scenes.get(p.frameSeqName).frames.get(i).crc32[0];
								System.arraycopy(crc, 0, vm.scenes.get(p.frameSeqName).frames.get(1).mask.data, k++*4, 4); //copy crc data to second frame because masks get shifted later
							}
							vm.scenes.get(p.frameSeqName).frames.get(1).mask.data = Frame.transform(vm.scenes.get(p.frameSeqName).frames.get(1).mask.data); // revert bit order for CRCs
						}
					}
					
					// TODO make this an attribute of switch mode
					frameSeq.reorderMask = (p.switchMode.equals(SwitchMode.FOLLOW) || p.switchMode.equals(SwitchMode.FOLLOWREPLACE ));
					frameSeqMap.put(p.frameSeqName, frameSeq);
					// TODO make this an attribute of switch mode
					if(p.switchMode.equals(SwitchMode.LAYEREDCOL) || p.switchMode.equals(SwitchMode.LAYEREDREPLACE)) {
						aniVersionForExport = 5;
					}
				} else {
					log.error("referenced scene not found, keyframe will be removed: {}", p);
					it.remove();
				}
			}
		}
		final int aniVersionToUse = aniVersionForExport;
		// VPIN
		if( !realPin ) {
			List<Animation> anis = new ArrayList<>();
			for (FrameSeq p : frameSeqMap.values()) {
				Animation ani = vm.scenes.get(p.name);
				// copy without extending frames / scaler does not matter
				CompiledAnimation cani = ani.cutScene(ani.start, ani.end, 0);
				cani.actFrame = 0;
				cani.setDesc(ani.getDesc());
				DMD tmp = new DMD(cani.width, cani.height);
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
				for( Mask m : ani.getMasks() ) {
					if( m.data.length != (vm.srcDmdSize.planeSize) )
						m.data = Arrays.copyOfRange(m.data,0,vm.srcDmdSize.planeSize);
					cani.getMasks().add(m);
				}
				anis.add(cani);
			}
			String aniFilename = replaceExtensionTo("vni", filename);
			Worker w = new Worker() {
				@Override
				public void innerRun() {
					AniWriter aniWriter = new AniWriter(anis, aniFilename, aniVersionToUse, vm.paletteMap, progress);
					if( !anis.isEmpty() ) {
						aniWriter.compressPlanes = false;
						aniWriter.setHeader("VPIN");
						aniWriter.run();
					}
					if( !cancelRequested ) {
						notify(90, "writing binary project");
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
					}
				}
			};
			progress.open(w);
			if( w.hasError() ) {
				messageUtil.warn(SWT.ICON_ERROR | SWT.OK ,
						"Error exporting project",
						"the following error occured while exporting: " + w.getRuntimeException().getMessage() + "\n");
			}
			
		} else {
			// for all referenced frame mapping we must also copy the frame data as
			// there are two models
			
			int actFrameTmp = 0;
			if (vm.selectedScene != null)
				actFrameTmp = vm.selectedScene.actFrame;
			
			for (FrameSeq p : frameSeqMap.values()) {
				CompiledAnimation ani = vm.scenes.get(p.name);
				ani.actFrame = 0;
				for (int i = 0; i <= ani.end; i++) {
					// copy before exporting
					Frame frame = new Frame(ani.frames.get(i));
					if( frame.planes.size() == Constants.TRUE_COLOR_BIT_PER_CHANNEL*3 ) { // reduce 8 bit per color to 5 bit per color
						log.debug("24 bit scene will reduced to 15 bit on export: {}", ani.getDesc());
						frame.planes.remove(0); frame.planes.remove(0); frame.planes.remove(0);
						frame.planes.remove(5); frame.planes.remove(5); frame.planes.remove(5);
						frame.planes.remove(10); frame.planes.remove(10); frame.planes.remove(10);
					} else {
						// remove planes not in plane mask
						int pl = 0;
						for (Iterator<Plane> iter = frame.planes.iterator(); iter.hasNext();) {
							iter.next();
							if (((1 << pl) & p.mask) == 0) {
								iter.remove();
							}
							pl++;
						}
					}
					// due to a bug in the current firmware, it is mandatory to have a mask in any case
					if( !frame.hasMask() ) {
						frame.setMask(new Mask(frame.planes.get(0).data.length)); // retrieve mask size from first plane
					}
					p.frames.add(frame);
				}
			}
			// create addtional files for frame sequences
			try {
				Map<String, Integer> map = new HashMap<String, Integer>();
				BinaryExporter exporter = BinaryExporterFactory.getInstance();
				if (!frameSeqMap.isEmpty()) {
					log.info("exporter instance {} writing FSQ", exporter);
					DataOutputStream dos = new DataOutputStream(streamProvider.buildStream(replaceExtensionTo("fsq", filename)));
					map = exporter.writeFrameSeqTo(dos, frameSeqMap, useOldExport?2:3);
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
			if (vm.selectedScene != null)
				vm.selectedScene.actFrame = actFrameTmp;
		}		
	}

	public void saveProject(String filename, boolean withProgress) {
		log.info("write project to {}", filename);
		String aniFilename = replaceExtensionTo("ani", filename);
		
		if(vm.selectedScene!=null) {
			vm.selectedScene.commitDMDchanges(vm.dmd); 
			vm.setDirty(vm.dirty|vm.selectedScene.isDirty());
		}

		if( vm.loadedAniVersion!= 0 && vm.loadedAniVersion < CURRENT_PRJ_ANI_VERSION ) {
			int res = messageUtil.warn(0, "Warning",
					"Older ani file format", 
					"This project animation file was written with an older version of the editor.\nSaving the project"
							+ " will convert it to the new format,\nthat can't be used with older editor versions.",
					new String[]{"", "Cancel", "Proceed"},2);
			if( res != 2 ) return;
		}
		
		vm.setLoadedAniVersion(CURRENT_PRJ_ANI_VERSION);
		
		if( backup ) {
			backupFiles(filename, aniFilename);
		}
		Project p = new Project();
		p.paletteMap.clear(); // TODO remove that in the CTOR
		populateVmToProject(vm, p);
		p.noOfPlanesWhenCutting = vm.noOfPlanesWhenCutting;
		
		String baseName = new File(aniFilename).getName();
		String baseNameWithoutExtension = baseName.substring(0, baseName.indexOf('.'));
		if (p.name == null) {
			p.name = baseNameWithoutExtension;
		} else if (!p.name.equals(baseNameWithoutExtension)) {
			// save as
			p.inputFiles.remove(p.name + ".ani");
		}
		p.setDimension(vm.prjDmdSize.width, vm.prjDmdSize.height);
		p.setSrcDimension(vm.srcDmdSize.width, vm.srcDmdSize.height);
		
		// we need to "tag" the projects animations that are always stored in the projects ani file
		// the project ani file is not included in the inputFile list but animations gets loaded
		// implicitly
		
		String path = new File(filename).getParent(); 
		// so first check directly included anis in project inputfiles
		for( String inFile : p.inputFiles) {
			Optional<Animation> optAni = vm.recordings.values().stream().filter(a -> a.getName().equals(path+File.separator+inFile)).findFirst();
			optAni.ifPresent(a-> {
				if( a.isDirty()) {
					aniAction.storeAnimations(Arrays.asList(a), a.getName(), 4, false, withProgress);
					a.setDirty(false);
				}
			});
		}
		
		storeOrDeleteProjectAnimations(aniFilename, withProgress);
		boolean isUsingLayeredColMask = false;
		for( PalMapping pm : vm.keyframes.values()) {
			if( pm.switchMode.equals(SwitchMode.LAYEREDCOL)) {
				isUsingLayeredColMask = true;
				break;
			}
		}
		// if layer col is not used so far we suppress writing exension attributes for backwards comp
		fileHelper.storeObject(p, filename, !isUsingLayeredColMask);
		
		vm.setDirty(false);
	}
	
	public void populateVmToProject(ViewModel vm, Project p) {
		// populate everything
		p.inputFiles.addAll(vm.inputFiles);
		if( vm.projectFilename != null ) p.name = bareName(vm.projectFilename);
		p.bookmarksMap.putAll(vm.bookmarksMap);
		p.mask = new byte[vm.prjDmdSize.planeSize];
		Arrays.fill(p.mask, (byte)0xFF);		// just for backwards comp. of older version of editor that expect something here
		p.palMappings.addAll(vm.keyframes.values());
		p.height = vm.prjDmdSize.height;
		p.width = vm.prjDmdSize.width;
		p.srcHeight = vm.srcDmdSize.height;
		p.srcWidth = vm.srcDmdSize.width;
		p.version = 2;
		p.paletteMap.putAll(vm.paletteMap);
		p.masks.clear();
		p.masks.addAll(vm.masks);
		p.recordingNameMap.putAll(vm.recordingNameMap);
	}

	private void backupFiles(String... filenames) {
		for(String file: filenames) {
			log.info("creating backup of file '{}'", file);
			try {
				if( new File(file).exists() ) {
					Path backupName = Paths.get(file+".bak");
					// if old backup file exists, remove it first
					if( backupName.toFile().exists()) {
						backupName.toFile().delete();
					}
					Files.copy( Paths.get(file), backupName);
				}
			} catch (IOException e) {
				log.warn("backup of {} failed", file, e);
			}
		}
	}

	private void storeOrDeleteProjectAnimations(String aniFilename, boolean withProgress) {
		// only need to save ani's that are 'project' animations
		List<Animation> prjAnis = vm.scenes.values().stream().filter(a->a.isProjectAnimation()).collect(Collectors.toList());
		if( !prjAnis.isEmpty() ) {
			aniAction.storeAnimations(prjAnis, aniFilename, CURRENT_PRJ_ANI_VERSION, true, withProgress);
		} else {
			new File(aniFilename).delete(); // delete project ani file
		}
	}



}
