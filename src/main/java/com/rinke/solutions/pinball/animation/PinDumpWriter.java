package com.rinke.solutions.pinball.animation;

import java.io.DataOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DeviceMode;
import com.rinke.solutions.pinball.Worker;
import com.rinke.solutions.pinball.model.Frame;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PinDumpWriter extends Worker {

	private CompiledAnimation a;
	private String filename;
	private int noOfPlanesToExport;


	public PinDumpWriter(CompiledAnimation ani, String filename, int noOfPlanesToExport, ProgressEventListener progressEvt) {
		this.a = ani;
		this.filename = filename;
		this.noOfPlanesToExport = noOfPlanesToExport;
		setProgressEvt(progressEvt);
	}


	@Override
	protected void innerRun() {
		DataOutputStream os = null;
		int tc = 0;
		//byte[] tcBuffer = new byte[4];
		try {
			log.info("exporting raw dump to {}",filename);
			notify(0,"exporting raw dump to " + filename);
			FileOutputStream fos = new FileOutputStream(filename);
			os = new DataOutputStream(fos);
			DMD dmd = new DMD(a.width,a.height);
			int numberOfFrames = a.getFrameCount(dmd);
			int total = numberOfFrames;
			// write header
			os.writeByte('R');
			os.writeByte('A');
			os.writeByte('W');
			os.writeByte(0x00);
			os.writeByte(0x01);
			os.writeByte(a.width);
			os.writeByte(a.height);
			os.writeByte(noOfPlanesToExport);
			while(numberOfFrames-- >0) { // frames of animation
				notify(((total-numberOfFrames)*100) / total,"exporting frames to " + filename);
				Frame frame =  a.render(dmd,false);
				// write timestamp
				os.writeInt(tc);
				tc += frame.delay;
				// write frame (subframe data)
				int plane = 0;
				while( plane < frame.planes.size() && plane < noOfPlanesToExport) {
					os.write( Frame.transform(frame.planes.get(plane++).data));
				}
			}
			os.close();
			log.info("exporting raw dump done");
		} catch( IOException e) {
			log.error("problems when wrinting file {}", filename);
		} finally {
			IOUtils.closeQuietly(os);
		}
		
	}

}
