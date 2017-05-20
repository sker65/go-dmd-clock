package com.rinke.solutions.pinball.view.handler;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.swt.SWT;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.BeanFactory;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.PinDmdEditor.OutputStreamProvider;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.api.BinaryExporter;
import com.rinke.solutions.pinball.api.BinaryExporterFactory;
import com.rinke.solutions.pinball.api.LicenseManager;
import com.rinke.solutions.pinball.api.LicenseManager.Capability;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.FrameSeq;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.Plane;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.model.Project;
import com.rinke.solutions.pinball.ui.GifExporter;
import com.rinke.solutions.pinball.util.ApplicationProperties;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class ExportHandler extends ViewHandler {

	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	FileChooserUtil fileChooserUtil;
	@Autowired
	LicenseManager licenseManager;
	
	@Autowired
	BeanFactory beanFactory;
	
	@Autowired
	ProjectHandler projectHandler;
	
	@Value(key=ApplicationProperties.OLDEXPORT)
	boolean useOldExport;

	public ExportHandler(ViewModel vm, Model m, CmdDispatcher d) {
		super(vm,m,d);
	}

	String replaceExtensionTo(String newExt, String filename) {
		int p = filename.lastIndexOf(".");
		if (p != -1)
			return filename.substring(0, p) + "." + newExt;
		return filename;
	}
	
	void exportProject(String filename, OutputStreamProvider streamProvider, boolean realPin) {
		log.info("export project {} file {}", realPin?"real":"vpin", filename);
		licenseManager.requireOneOf(Capability.VPIN, Capability.REALPIN, Capability.GODMD);

		// TODO rebuild frame seq map why in model? 
		Map<String,FrameSeq> frameSeqMap = new HashMap<String, FrameSeq>();

		frameSeqMap.clear();
		for (PalMapping p : model.palMappings) {
			if (p.frameSeqName != null) {
				FrameSeq frameSeq = new FrameSeq(p.frameSeqName);
				if (p.switchMode.equals(SwitchMode.ADD) || p.switchMode.equals(SwitchMode.FOLLOW) ) {
					frameSeq.mask = 0b11111100;
				}
				frameSeqMap.put(p.frameSeqName, frameSeq);
			}
		}
		
		// VPIN
		if( !realPin ) {
			List<Animation> anis = new ArrayList<>();
			for (FrameSeq p : frameSeqMap.values()) {
				Animation ani = model.scenes.get(p.name);
				// copy without extending frames
				CompiledAnimation cani = ani.cutScene(ani.start, ani.end, 0);
				cani.actFrame = 0;
				cani.setDesc(ani.getDesc());
				DMD tmp = new DMD(model.dmdSize.width, model.dmdSize.height);
				for (int i = cani.start; i <= cani.end; i++) {
					cani.getCurrentMask();
					Frame f = cani.render(tmp, false);
					for( int j = 0; j < f.planes.size(); j++) {
						if (((1 << j) & p.mask) == 0) {
							Arrays.fill(f.planes.get(j).data, (byte)0);
						}
					}
				}
				anis.add(cani);
			}
			if( !anis.isEmpty() ) {
				String aniFilename = replaceExtensionTo("vni", filename);
				AniWriter aniWriter = new AniWriter(anis, aniFilename, 4, model.palettes, null);
				aniWriter.setHeader("VPIN");
				aniWriter.run();
				try {
					BinaryExporter exporter = BinaryExporterFactory.getInstance();
					DataOutputStream dos2 = new DataOutputStream(streamProvider.buildStream(filename));
					// for vpins version is 2
					exporter.writeTo(dos2, aniWriter.getOffsetMap(), projectHandler.buildProjectFrom(model,2));
					dos2.close();
				} catch (IOException e) {
					throw new RuntimeException("error writing " + filename, e);
				}
			}
			
		} else {
			// for all referenced frame mapping we must also copy the frame data as
			// there are two models
			for (FrameSeq p : frameSeqMap.values()) {
				CompiledAnimation ani = model.scenes.get(p.name);				
				ani.actFrame = 0;
				DMD tmp = new DMD(model.dmdSize.width, model.dmdSize.height);
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
				Project project = projectHandler.buildProjectFrom(model, 3);

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
	
	// export handler
	public void onExportVpinProject() {
		licenseManager.requireOneOf(Capability.VPIN, Capability.GODMD);
		String filename = fileChooserUtil.choose(SWT.SAVE, model.filename, new String[] { "*.pal" }, new String[] { "Export pal" });
		if (filename != null) {
			messageUtil.warn("Warning", "Please don´t publish projects with copyrighted material / frames");
			exportProject(filename, f -> new FileOutputStream(f), false);
		}
	}
	
	public void onExportRealpinProject() {
		licenseManager.requireOneOf( Capability.REALPIN, Capability.GODMD);
		String filename = fileChooserUtil.choose(SWT.SAVE, model.filename, new String[] { "*.pal" }, new String[] { "Export pal" });
		if (filename != null) {
			messageUtil.warn("Warning", "Please don´t publish projects with copyrighted material / frames");
			exportProject(filename, f -> new FileOutputStream(f), true);
			if( !filename.endsWith("pin2dmd.pal")) {
				messageUtil.warn("Hint", "Remember to rename your export file to pin2dmd.pal if you want to use it" + " in a real pinballs sdcard of pin2dmd.");
			}
		}
		
	}
	
	public void onExportGoDMD() {
		messageUtil.warn("not implemented", "sorry but this is not yet implemented");
	}
	
	public void onExportGif() {
		GifExporter exporter = beanFactory.getBeanByType(GifExporter.class);
		exporter.setAni(vm.playingAni);
		exporter.setPalette((Palette) vm.palettes.get(vm.playingAni.getPalIndex()));
		exporter.open();		
	}

	
}
