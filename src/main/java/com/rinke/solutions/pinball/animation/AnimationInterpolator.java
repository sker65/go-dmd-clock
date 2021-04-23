package com.rinke.solutions.pinball.animation;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;

public class AnimationInterpolator {
	
	private Map<Frame,Mat> mat2frame = new HashMap<>();
	private Palette pal;
	private int h;
	private int w;

	public AnimationInterpolator(CompiledAnimation src, Palette pal) {
		this.w = src.width;
		this.h = src.height;
		this.pal = pal;
	}

	/**
	 * check if scene can interpolated: there must be at least one keyframe
	 * 
	 * @param src
	 *            ani to check
	 * @return null for okay, error message if not
	 */
	public String validate(CompiledAnimation src) {
		int keyFrames = 0;
		for (Frame frame : src.frames) {
			if (frame.keyFrame)
				keyFrames++;
		}
		if (keyFrames <= 1)
			return "Scene has no (or only one) keyframes";
		return null;
	}

	// see https://stackoverflow.com/questions/14958643/converting-bufferedimage-to-mat-in-opencv
	public static Mat bufferedImageToMat(BufferedImage bi) {
		Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		mat.put(0, 0, data);
		return mat;
	}
	
	private Mat getMatfromFrame(Frame f) {
		Mat m = mat2frame.get(f);
		if( m != null ) return m;
		m = bufferedImageToMat(bufferedImagefromFrame(f,this.w, this.h, this.pal));
		mat2frame.put(f, m);
		return m;
	}

	
	public static BufferedImage bufferedImagefromFrame(Frame f, int w, int h, Palette p) {
		BufferedImage bi = new BufferedImage(w,h,BufferedImage.TYPE_3BYTE_BGR); // use BGR color model as expected by opencv
		int numberOfSubframes = f.planes.size();
		// int bitsPerColorChannel = numberOfSubframes / 3;
		//int cmask = 0xFF >> (8-bitsPerColorChannel);
		int bytesPerRow = w / 8; // only work with byte 8-aligned width
		for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                // lsb first
                // byte mask = (byte) (1 << (col % 8));
                // hsb first
                byte mask = (byte) (0b10000000 >> (col % 8));
                int v = 0;
                for(int i = 0; i < numberOfSubframes;i++) {
                	if( col / 8 + row * bytesPerRow < f.getPlane(i).length) {
                		v += (f.getPlane(i)[col / 8 + row * bytesPerRow] & mask) != 0 ? (1<<i) : 0;
                	}
                }
                // TODO add sanity check that v is in range
                RGB rgb = p.colors[v];
                bi.setRGB(row, col, rgb.red << 16 + rgb.green << 8 + rgb.blue );
            }
        }
		return bi;
	}

	public CompiledAnimation interpolate(String name, CompiledAnimation src, Palette pal) {
		// todo copy src
		for (int i = 0; i < src.frames.size(); i++) {
			if (src.frames.get(i).keyFrame) {
				for (int j = i + 1; i < src.frames.size(); j++) {
					if (src.frames.get(j).keyFrame) {
						List<Frame> interpolatedFrames = this.interpolateFrames(src.frames, i, j, pal, src.width, src.height);
					}
				}
			}
		}
		return null;
	}

	private List<Frame> interpolateFrames(List<Frame> frames, int startKeyFrame, int endKeyFrame, Palette pal, int w, int h) {
		int nextLowToInterpolate = startKeyFrame + 1;
		int nextHighToInterpolate = endKeyFrame - 1;
		List<Frame> result = new ArrayList<>();
		while (nextHighToInterpolate - nextLowToInterpolate >= 0) {
			double histHighDiff = this.getBestNeighborhoodKeyframe(frames.get(nextHighToInterpolate), frames.get(nextHighToInterpolate + 1), pal, w, h);
			double histLowDiff = this.getBestNeighborhoodKeyframe(frames.get(nextLowToInterpolate - 1), frames.get(nextLowToInterpolate), pal, w, h);
			if (histHighDiff < histLowDiff) {
				Frame f = this.propagateFrameFromTo(frames.get(nextHighToInterpolate + 1), frames.get(nextHighToInterpolate), pal);
			} else {
				Frame f = this.propagateFrameFromTo(frames.get(nextLowToInterpolate - 1), frames.get(nextLowToInterpolate), pal);
			}
			// TODO insert frame at the right pos!!
		}
		return result;
	}

	private Frame propagateFrameFromTo(Frame srcframe, Frame frameToInterpolate, Palette pal) {
		// calc motion flow
		// flow = cv2.calcOpticalFlowFarneback(img_frame1, img_frame2, None, 0.5, 3, 15, 3, 7, 1.5, 0)
		Mat prvs = getMatfromFrame(srcframe);
		Mat next = getMatfromFrame(frameToInterpolate);
        Mat flow = new Mat(prvs.size(), CvType.CV_32FC2);
        Video.calcOpticalFlowFarneback(prvs, next, flow, 0.5, 3, 15, 3, 7, 1.5, 0);
        for(int x = 0; x <= this.w; x++) {
        	for( int y = 0; y < this.h; y++) {
                int dx = (int) Math.round(flow.get(x,y)[1]);
                int dy = (int) Math.round(flow.get(x,y)[0]);
                int ppx = x - dx;
                int ppy = y - dy;
                if( ppx < 0)  //  # curate edges of frame
                	ppx = 0;
                if( ppx > this.w - 1)
                	ppx = this.w - 1;
                if( ppy < 0)
                	ppy = 0;
                if( ppy > this.h - 1)
                	ppy = this.h - 1;
                
        	}
        }
		return null;
	}

	private double getBestNeighborhoodKeyframe(Frame frame1, Frame frame2, Palette pal, int w, int h) {
		// done by histogram_difference_between_frames
		float[] range = { 0, 256 }; // the upper boundary is exclusive
		MatOfFloat histRange = new MatOfFloat(range);
		MatOfInt hist1 = new MatOfInt(), hist2 = new MatOfInt();
		List<Mat> bgrPlanes1 = new ArrayList<>();
		//  add converted frame
		bgrPlanes1.add(getMatfromFrame(frame1));
		Imgproc.calcHist(bgrPlanes1, new MatOfInt(0), new Mat(), null, hist1, histRange);
		List<Mat> bgrPlanes2 = new ArrayList<>();
		//  add converted frame2
		bgrPlanes2.add(getMatfromFrame(frame2));
		Imgproc.calcHist(bgrPlanes2, new MatOfInt(0), new Mat(), null, hist2, histRange);
		return Imgproc.compareHist(hist1, hist2, 3);
	}


}
