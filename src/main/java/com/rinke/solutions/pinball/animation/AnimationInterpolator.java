package com.rinke.solutions.pinball.animation;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnimationInterpolator {

	private Map<Frame, Mat> mat2frame = new HashMap<>();
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

	// see
	// https://stackoverflow.com/questions/14958643/converting-bufferedimage-to-mat-in-opencv
	public static Mat bufferedImageToMat(BufferedImage bi) {
		Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		mat.put(0, 0, data);
		return mat;
	}

	private Mat getMatfromFrame(Frame f) {
		Mat m = mat2frame.get(f);
		if (m != null)
			return m;
		byte[] data = bgrImageDatafromFrame(f, this.w, this.h, this.pal);
		m = new Mat(this.h, this.w, CvType.CV_8UC3);
		m.put(0, 0, data);
		// log.debug("write {}",f.hashCode());
		// Imgcodecs.imwrite("img"+f.hashCode()+".png", m);
		mat2frame.put(f, m);
		return m;
	}

	public static byte[] bgrImageDatafromFrame(Frame f, int w, int h, Palette p) {
		byte[] bi = new byte[w * h * 3]; // use BGR color model as expected by
											// opencv
		int numberOfSubframes = f.planes.size();
		log.debug("creating buffered image w={}, h={}, subframes={}, p={}", w, h, numberOfSubframes, p.name);
		// int bitsPerColorChannel = numberOfSubframes / 3;
		// int cmask = 0xFF >> (8-bitsPerColorChannel);
		int bytesPerRow = w / 8; // only work with byte 8-aligned width
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				// lsb first
				// byte mask = (byte) (1 << (col % 8));
				// hsb first
				byte mask = (byte) (0b10000000 >> (x % 8));
				int v = 0;
				for (int i = 0; i < numberOfSubframes; i++) {
					if (x / 8 + y * bytesPerRow < f.getPlane(i).length) {
						v += (f.getPlane(i)[x / 8 + y * bytesPerRow] & mask) != 0 ? (1 << i) : 0;
					}
				}
				// TODO add sanity check that v is in range
				RGB rgb = p.colors[v];
				// bi.setRGB(x, y, rgb.red << 16 + rgb.green << 8 + rgb.blue );
				int j = w * y * 3 + x * 3;
				bi[j] = (byte) rgb.blue;
				bi[j + 1] = (byte) rgb.green;
				bi[j + 2] = (byte) rgb.red;
			}
		}
		return bi;
	}

	// AK : this is the old version, much more elegant, but I could not manage
	// to make it work :-(
	/*public CompiledAnimation interpolate(String name, CompiledAnimation src, Palette pal) {
		log.debug("Starting Interpolate");
		CompiledAnimation res = src.cutScene(src.start, src.end, src.getNumberOfPlanes());
		for (int i = 0; i < res.frames.size(); i++) {
			if (res.frames.get(i).keyFrame) {
				for (int j = i + 1; j < res.frames.size(); j++) {
					if (res.frames.get(j).keyFrame) {
						this.interpolateFrames2(res.frames, i, j, pal, res.width, res.height);
						i = j - 1;
						break;
					}
				}
			}
		}
		return res;
	}*/

	// AK: this is pretty terrible, but works - no idea why the old above
	// function wouldnt do what it should.
	// The Problem: interpolateFrames2 does only work if I the first
	// keyframe-index is 0. I suspect some memory mismanagement from my side.
	// This hack, starts all calls with index 0 and then copies the results from
	// cache.
	public CompiledAnimation interpolate(String name, CompiledAnimation src, Palette pal) {
		log.debug("Starting Interpolate");
		CompiledAnimation res = src.cutScene(src.start, src.end, src.getNumberOfPlanes());
		for (int i = 0; i < res.frames.size(); i++) {
			if (res.frames.get(i).keyFrame) {
				for (int j = i + 1; j < res.frames.size(); j++) {
					if (res.frames.get(j).keyFrame) {
						CompiledAnimation cache = src.cutScene(i, j, src.getNumberOfPlanes());
						this.interpolateFrames2(cache.frames, 0, j - i, pal, res.width, res.height);
						for (int k = 0; k < cache.frames.size(); k++) {
							if (!(cache.frames.get(k).keyFrame)) {
								res.frames.set(i + k, cache.frames.get(k));
							}
						}
						i = j - 1;
						break;
					}
				}
			}
		}
		return res;
	}

	// AK: New InterpolateFrames routine, replaces old one
	private List<Frame> interpolateFrames2(List<Frame> frames, int startKeyFrame, int endKeyFrame, Palette pal, int w, int h) {
		log.debug("Start Interpolate Frames2");
		List<Mat> flowMats = new ArrayList<>();
		List<Frame> result = new ArrayList<>();
		// Frame keyLowFrame = frames.get(startKeyFrame);
		// Frame keyHighFrame = frames.get(endKeyFrame);
		int actualKeyFrame = 0;
		int paletteSize = 4;
		// Construct list of Flow Mats - we need this to sum flow vectors from
		// both key frames and compare.
		for (int i = 0; i < (endKeyFrame - startKeyFrame); i++) {
			Mat msrc = getMatfromFrame(frames.get(i));
			Mat minter = getMatfromFrame(frames.get(i + 1));
			Mat flow = new Mat(msrc.size(), CvType.CV_32FC2);
			Mat minter_gray = new Mat(minter.size(), CvType.CV_8UC1);
			Mat msrc_gray = new Mat(msrc.size(), CvType.CV_8UC1);
			Imgproc.cvtColor(minter, minter_gray, Imgproc.COLOR_BGR2GRAY);
			Imgproc.cvtColor(msrc, msrc_gray, Imgproc.COLOR_BGR2GRAY);
			Video.calcOpticalFlowFarneback(msrc_gray, minter_gray, flow, 0.5, 3, 15, 3, 7, 1.5, 0);
			flowMats.add(flow);
			msrc_gray.release();
			minter_gray.release();
			// flow.release();
		}

		// int nextLowToInterpolate = startKeyFrame + 1;
		// int nextHighToInterpolate = endKeyFrame - 1;
		log.debug("startKeyFrame={}, endKeyFrame={}", startKeyFrame, endKeyFrame);
		for (int i = 1; i < (endKeyFrame - startKeyFrame); i++) {
			int iFrame = startKeyFrame + i;
			Frame propagatedFrame = new Frame(frames.get(startKeyFrame + i));
			log.debug("startKeyFrame={}, endKeyFrame={}, interpolated_frame={}", startKeyFrame, endKeyFrame, startKeyFrame + i);

			for (int r = 0; r < h; r++) {
				for (int c = 0; c < w; c++) {
					// Compute added Optical Flow vector from High Key
					int[] dVec = computeFlowVector(c, r, endKeyFrame, startKeyFrame + i, flowMats);
					int dr = dVec[1];
					int dc = dVec[0];
					// log.debug("x({})={},dy({})={}",c, r, dVec[0],dVec[1]);
					double dlengthHigh = Math.sqrt(dr * dr + dc * dc);
					int ppcH = c + dc;
					int pprH = r + dr;
					// Compute added Optical Flow vector from Low Key
					dVec = computeFlowVector(c, r, startKeyFrame, startKeyFrame + i, flowMats);
					dr = dVec[1];
					dc = dVec[0];
					double dlengthLow = Math.sqrt(dr * dr + dc * dc);
					int ppcL = c - dc;
					int pprL = r - dr;
					// ho is true if vector points out of image for High_Key,
					// respectively, lo is True for low_key.
					boolean ho = (pprH < 0 || pprH > (h - 1) || ppcH < 0 || ppcH > (w - 1));
					boolean lo = (pprL < 0 || pprL > (h - 1) || ppcL < 0 || ppcL > (w - 1));
					boolean direction = true;
					// ho True, lo not, take low_key
					if (ho && !lo) {
						direction = false;
					}
					// # lo True, ho not, take high_key
					if (lo && !ho) {
						direction = true;
					}
					// both are neither True or False, then use criterium of
					// vector length, shorter equals better
					if ((ho && lo) || (!ho && !lo)) {
						if (dlengthLow < dlengthHigh) {
							direction = false;
						} else {
							direction = true;
						}
					}
					int ppc = 0;
					int ppr = 0;

					// # Depending on direction use the correct values for ppc
					// and ppr and set the correct high/low keyframe
					// # if High_key is used

					if (direction) {
						ppc = ppcH;
						ppr = pprH;
						actualKeyFrame = endKeyFrame;
					} else {
						ppc = ppcL;
						ppr = pprL;
						actualKeyFrame = startKeyFrame;
					}
					// curate edges, hopefully not needed
					if (ppr < 0) {
						ppr = 0;
					}
					if (ppr > h - 1) {
						ppr = h - 1;
					}
					if (ppc < 0) {
						ppc = 0;
					}
					if (ppc > w - 1) {
						ppc = w - 1;
					}
					// Propagate Palette of size=paletteSize
					int colIdx = getPixel(frames.get(actualKeyFrame), ppc, ppr, this.w / 8) / paletteSize;
					int colIdx2 = getPixel(frames.get(iFrame), c, r, this.w / 8) % paletteSize;
					setPixel(propagatedFrame, colIdx2 + colIdx * paletteSize, c, r, this.w / 8);

					// Alternatively use the following block of code:

					// Propagate each color value pixel-wise.
					// int colIdx = getPixel(actualKeyFrame, ppc, ppr, this.w/8;
					// setPixel(propagatedFrame, colIdx, c, r, this.w/8);

				}
			}
			frames.set(iFrame, propagatedFrame);

			releaseCachedFrames();

		}
		releaseListMats(flowMats);
		return result;
	}

	// AK: Routine to compute the added flow-Vector from a startidx to an
	// endindex. Startindex refers to a Keyframe, endindex to an iFrame.
	private int[] computeFlowVector(int c, int r, int startIdx, int endIdx, List<Mat> flowMats) {
		double[] d_vec = new double[2];
		BigDecimal bdx = new BigDecimal("0");
		BigDecimal bdy = new BigDecimal("0");

		double dx = 0;
		double dy = 0;
		int lenFlowMats = flowMats.size();
		int seqLen = endIdx - startIdx;
		if (seqLen > 0) {
			// if startindex is smaller than endindex, than count forward.
			for (int i = 0; i < seqLen; i++) {
				Mat fM = flowMats.get(i);
				d_vec = fM.get(r, c);
				bdx = bdx.add(BigDecimal.valueOf(d_vec[0]));
				bdy = bdy.add(BigDecimal.valueOf(d_vec[1]));
				dx = dx + d_vec[0];
				dy = dy + d_vec[1];

			}
			// else count backwards.
		} else {
			for (int i = lenFlowMats - 1; i >= lenFlowMats + seqLen; i--) {
				Mat fM = flowMats.get(i);
				d_vec = fM.get(r, c);
				bdx = bdx.add(BigDecimal.valueOf(d_vec[0]));
				bdy = bdy.add(BigDecimal.valueOf(d_vec[1]));
				dx = dx + d_vec[0];
				dy = dy + d_vec[1];

			}
		}
		int[] returnVec = new int[2];
		// log.debug("D-Vector from Frame:{} to Frame:{} for SUM
		// dx({})={},dy({})={}", startIdx, endIdx, c, dx, r,dy);
		// returnVec[0] = symmetricRound(dx);
		// returnVec[1] = symmetricRound(dy);
		bdx = bdx.setScale(0, BigDecimal.ROUND_HALF_EVEN);
		bdy = bdy.setScale(0, BigDecimal.ROUND_HALF_EVEN);
		returnVec[0] = bdx.intValue();
		returnVec[1] = bdy.intValue();
		// log.debug("D-Vector from Frame:{} to Frame:{} for
		// dx({})={},dy({})={}", startKey, endKey, c, r,
		// returnVec[0],returnVec[1]);
		return returnVec;
	}

	private void releaseCachedFrames() {
		for (Mat m : this.mat2frame.values())
			m.release();
		this.mat2frame.clear();
	}

	private void releaseListMats(List<Mat> FlowMats) {
		for (int i = 0; i < FlowMats.size(); i++) {
			Mat m = FlowMats.get(i);
			m.release();
		}
	}

	private void setPixel(Frame r, int v, int x, int y, int bytesPerRow) {
		byte mask = (byte) (0b10000000 >> (x % 8));
		int numberOfPlanes = r.planes.size();
		for (int plane = 0; plane < numberOfPlanes; plane++) {
			if ((v & 0x01) != 0) {
				r.planes.get(plane).data[y * bytesPerRow + x / 8] |= mask;
			} else {
				r.planes.get(plane).data[y * bytesPerRow + x / 8] &= ~mask;
			}
			v >>= 1;
		}
	}

	private int getPixel(Frame frame, int x, int y, int bytesPerRow) {
		byte mask = (byte) (0b10000000 >> (x % 8));
		int v = 0;
		for (int plane = 0; plane < frame.planes.size(); plane++) {
			v += (frame.planes.get(plane).data[x / 8 + y * bytesPerRow] & mask) != 0 ? (1 << plane) : 0;
		}
		return v;
	}

}