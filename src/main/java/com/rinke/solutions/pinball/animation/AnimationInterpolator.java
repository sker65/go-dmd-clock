package com.rinke.solutions.pinball.animation;

import java.util.List;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;

public class AnimationInterpolator {

	/**
	 * check if scene can interpolated: there must be at least one keyframe
	 * @param src ani to check
	 * @return null for okay, error message if not
	 */
	public String validate(CompiledAnimation src) {
		int keyFrames = 0;
		for (Frame frame : src.frames) {
			if( frame.keyFrame ) keyFrames++;
		}
		if( keyFrames <= 1 ) return "Scene has no (or only one) keyframes";
		return null;
	}

	public CompiledAnimation interpolate(String name, CompiledAnimation src, Palette selectedPalette) {
		for( int i = 0; i < src.frames.size(); i++) {
			if( src.frames.get(i).keyFrame ) {
				for( int j = i+1; i < src.frames.size(); j++) {
					if( src.frames.get(j).keyFrame ) {
						List<Frame> interpolatedFrames = this.interpolateFrame(src.frames, i, j);
					}
				}
			}
		}
		return null;
	}

	private List<Frame> interpolateFrame(List<Frame> frames, int startFrame, int endFrame) {
		
		return null;
	}

}
