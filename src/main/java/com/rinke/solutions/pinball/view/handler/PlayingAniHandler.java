package com.rinke.solutions.pinball.view.handler;

import java.util.List;

import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DMDClock;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.CmdDispatcher.Command;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
public class PlayingAniHandler extends ViewHandler {

	public PlayingAniHandler(ViewModel vm, Model model, CmdDispatcher d) {
		super(vm, model, d);
	}
	
	private Animation ani;
	private boolean clockActive;
	private boolean forceRerender;
	private boolean stop;
	private int lastRenderedFrame;
	private boolean enableClock;
	DMDClock clock = new DMDClock(false);
	
	public void onPlayingAniChanged(Animation ov, Animation nv) {
		// update dmd, actFrame
		// -> must result in redraw of widget
		if( nv == null ) {
			vm.dmd.clear();
		} else {
			this.ani = nv;
			vm.setMinFrame(ani.start);
			vm.setMaxFrame(ani.end);
			vm.setFrameIncrement(ani.skip);
			vm.setPrevEnabled(ani!=null && vm.actFrame>vm.minFrame);
			vm.setNextEnabled(ani!=null && vm.actFrame<=vm.maxFrame);
			renderAni();
		}
		vm.setStartStopEnabled(nv!=null);
	}
	
	public void onPrevFrame() {
		vm.setActFrame(vm.actFrame-1);
	}
	
	public void onNextFrame() {
		vm.setActFrame(vm.actFrame+1);
	}
	
	public void onActFrameChanged(int ov, int nv) {
		if( nv != ani.actFrame ) {
			Frame f = renderAni();
			if( ani.end != vm.maxFrame ) { // sometimes end gets updated while rendering
				vm.setMaxFrame(ani.end);
			}
			vm.setDelay(ani.getRefreshDelay());
			vm.setTimecode((int) ani.getTimeCode(ani.actFrame));
			updateHashes(f);
		}
		vm.setPrevEnabled(ani!=null && vm.actFrame>vm.minFrame);
		vm.setNextEnabled(ani!=null && vm.actFrame<=vm.maxFrame);
		ani.actFrame = nv;
	}

	private void updateHashes(Frame frame) {
		if( frame == null ) return;
		Frame f = new Frame(frame);
		Mask currentMask = getCurrentMask();
		if( vm.maskVisible && currentMask != null) {
			f.setMask(getCurrentMask().data);
		}

		dispatcher.dispatch(new Command<List<byte[]>>(f.getHashes(), "updateHashes"));
	}

	// TODO 
	private Mask getCurrentMask() {
		return null;
	}

	private Frame renderAni() {
		Frame res = null;
		if( clockActive ) {
		} else {			
			if( !forceRerender  && stop && ani.actFrame == lastRenderedFrame ) return null;
			
			forceRerender = false;
			vm.dmd.clear();
			if( enableClock && ani.addClock() ) {
				ani.setClockWasAdded(true);
			    if( ani.isClockSmall())
			        clock.renderTime(vm.dmd, ani.isClockSmall(), ani.getClockXOffset(),ani.getClockYOffset(),false);
			    else
			        clock.renderTime(vm.dmd,false);
			}
			res = ani.render(vm.dmd,stop);
            //vm.setActFrame(ani.actFrame);
            lastRenderedFrame = ani.actFrame;
            
            // only dmd playback nothing goDMD like
            if( false && res.hasMask() ) { // there is a mask
                if( ani.getClockFrom()>ani.getTransitionFrom())
                	vm.dmd.writeNotAnd(res.planes.get(2).data); // mask out clock
                DMD tmp = new DMD(vm.dmd.getWidth(), vm.dmd.getHeight());
                tmp.writeOr(res);
                tmp.writeAnd(res.planes.get(2).data);       // mask out ani
                vm.dmd.writeOr(tmp.getFrame()); // merge
            } else {
                // now if clock was rendered, use font mask to mask out digits in animation
                if( enableClock && ani.addClock() ) {
                    DMD tmp = new DMD(vm.dmd.getWidth(), vm.dmd.getHeight());
                    tmp.writeOr(res);
                    clock.renderTime(tmp,true); // mask out time
                    vm.dmd.writeOr(tmp.getFrame());
                } else {
                	vm.dmd.writeOr(res);
                }
            }
	
			if( ani.hasEnded() ) {
				ani.restart();
				//if(showClock) setClockActive(true);
			}

		}
		return res;
	}

}
