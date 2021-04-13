package com.rinke.solutions.pinball.animation;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

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

	public CompiledAnimation interpolate(String name, CompiledAnimation src, Palette pal) {
		for( int i = 0; i < src.frames.size(); i++) {
			if( src.frames.get(i).keyFrame ) {
				for( int j = i+1; i < src.frames.size(); j++) {
					if( src.frames.get(j).keyFrame ) {
						List<Frame> interpolatedFrames = this.interpolateFrames(src.frames, i, j, pal);
					}
				}
			}
		}
		return null;
	}

	private List<Frame> interpolateFrames(List<Frame> frames, int startFrame, int endFrame, Palette pal) {
		int nextLowToInterpolate = startFrame+1;
		int nextHighToInterpolate = endFrame-1;
		List<Frame> result = new ArrayList<>();
		while( nextHighToInterpolate-nextLowToInterpolate >= 0) {
			double histHighDiff = this.getBestNeighborhoodKeyframe(frames.get(nextHighToInterpolate),frames.get(nextHighToInterpolate+1), pal);
			double histLowDiff = this.getBestNeighborhoodKeyframe(frames.get(nextLowToInterpolate-1),frames.get(nextLowToInterpolate), pal);
			if( histHighDiff < histLowDiff) {
				Frame f = this.propagateFrameFromTo(frames.get(nextHighToInterpolate+1), frames.get(nextHighToInterpolate), pal);
			} else {
				Frame f = this.propagateFrameFromTo(frames.get(nextLowToInterpolate-1),frames.get(nextLowToInterpolate), pal);
			}
			// TODO insert frame at the right pos!!
		}
		return result;
	}

	private Frame propagateFrameFromTo(Frame frame, Frame frame2, Palette pal) {
		return null;
	}

	private double getBestNeighborhoodKeyframe(Frame frame1, Frame frame2, Palette pal) {
		// done by histogram_difference_between_frames
		float[] range = {0, 256}; //the upper boundary is exclusive
        MatOfFloat histRange = new MatOfFloat(range);
        MatOfInt hist1 = new MatOfInt(), hist2 = new MatOfInt();
        List<Mat> bgrPlanes1 = new ArrayList<>();
        // TODO add converted frame1
        Imgproc.calcHist(bgrPlanes1, new MatOfInt(0), new Mat(), null, hist1, histRange);
        List<Mat> bgrPlanes2 = new ArrayList<>();
        // TODO add converted frame1
        Imgproc.calcHist(bgrPlanes2, new MatOfInt(0), new Mat(), null, hist2, histRange);
		return Imgproc.compareHist( hist1, hist2, 3 );
	}

}
