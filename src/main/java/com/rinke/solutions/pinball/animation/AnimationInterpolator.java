package com.rinke.solutions.pinball.animation;

import java.util.ArrayList;
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
						List<Frame> interpolatedFrames = this.interpolateFrames(src.frames, i, j);
					}
				}
			}
		}
		return null;
	}

	private List<Frame> interpolateFrames(List<Frame> frames, int startFrame, int endFrame) {
		int nextLowToInterpolate = startFrame+1;
		int nextHighToInterpolate = endFrame-1;
		List<Frame> result = new ArrayList<>();
		while( nextHighToInterpolate-nextLowToInterpolate >= 0) {
			float histHighDiff = this.getBestNeighborhoodKeyframe(frames.get(nextHighToInterpolate),frames.get(nextHighToInterpolate+1));
			float histLowDiff = this.getBestNeighborhoodKeyframe(frames.get(nextLowToInterpolate-1),frames.get(nextLowToInterpolate));
			if( histHighDiff < histLowDiff) {
				Frame f = this.propagateFrameFromTo(frames.get(nextHighToInterpolate+1), frames.get(nextHighToInterpolate));
			} else {
				Frame f = this.propagateFrameFromTo(frames.get(nextLowToInterpolate-1),frames.get(nextLowToInterpolate));
			}
			// TODO insert frame at the right pos!!
		}
		return result;
	}

	private Frame propagateFrameFromTo(Frame frame, Frame frame2) {
		return null;
	}

	private float getBestNeighborhoodKeyframe(Frame frame, Frame frame2) {
		// done by histogram_difference_between_frames
		return 0;
	}

}
