package com.rinke.solutions.pinball.animation;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DmdSize;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Plane;


public class CompiledAnimationTest {

	CompiledAnimation uut = null;
	DmdSize size = DmdSize.Size128x32;
	@Before
	public void setup() {
		Animation ani = AnimationFactory.buildAnimationFromFile("./src/test/resources/drwho-dump.txt.gz", AnimationType.MAME);
		DMD dmd = new DMD(size);
		dmd.setNumberOfPlanes(4);
		ani.render(dmd, false);
		uut = (CompiledAnimation) ani.cutScene(30, 200, 4);
		uut.setDesc("foo");
	}
	
	@Test
	public void testCommitDMDchanges() throws Exception {
		DMD dmd = new DMD(size);
		dmd.setNumberOfPlanes(4);
		Frame frame = uut.render(dmd , true);
		byte sum=0;
		for (Plane p : frame.planes) {
			for(byte b : p.data) sum +=b;
		}
		assertEquals(-2,sum);
		
		dmd.addUndoBuffer();
		dmd.setPixel(0, 0, 1);
		
		uut.commitDMDchanges(dmd);
		dmd.clear();
		
		frame = uut.render(dmd , true);
		sum=0;
		for (Plane p : frame.planes) {
			for(byte b : p.data) sum +=b;
		}

		assertEquals(-128,sum);
		
	}

}
