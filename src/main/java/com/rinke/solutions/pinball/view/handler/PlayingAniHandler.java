package com.rinke.solutions.pinball.view.handler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;

import lombok.extern.slf4j.Slf4j;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.beans.Value;
import com.rinke.solutions.pinball.DMD;
import com.rinke.solutions.pinball.DMDClock;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.animation.EditMode;
import com.rinke.solutions.pinball.model.Frame;
import com.rinke.solutions.pinball.model.Mask;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.model.RGB;
import com.rinke.solutions.pinball.model.PalMapping.SwitchMode;
import com.rinke.solutions.pinball.swt.TimerExec;
import com.rinke.solutions.pinball.util.ApplicationProperties;
import com.rinke.solutions.pinball.util.Config;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.util.ObservableMap;
import com.rinke.solutions.pinball.view.CmdDispatcher;
import com.rinke.solutions.pinball.view.CmdDispatcher.Command;
import com.rinke.solutions.pinball.view.model.CutInfo;
import com.rinke.solutions.pinball.view.model.Model;
import com.rinke.solutions.pinball.view.model.TypedLabel;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class PlayingAniHandler extends ViewHandler {

	@Autowired
	TimerExec timerExec;
	
	@Value(key=Config.NOOFPLANES, defaultValue="4")
	int numberOfPlanes;
		
	@Autowired
	SceneHandler sceneHandler;
	@Autowired
	KeyFrameHandler keyFrameHandler;
	
	@Autowired
	PaletteHandler paletteHandler;
	@Autowired
	MessageUtil messageUtil;
	
	@Value(key=Config.AUTOKEYFRAME)
	boolean autoCreateKeyframe;
		
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
		vm.setStartStopEnabled(nv!=null);
		vm.setAnimationIsPlaying(false);
		if( nv == null ) {
			vm.dmd.clear();
		} else {
			this.ani = nv;
			this.ani.setActFrame(vm.selectedFrame);
			vm.setMinFrame(ani.start);
			vm.setMaxFrame(ani.end);
			vm.setSkip(ani.skip);
			vm.setNumberOfPlanes(ani.getRenderer().getNumberOfPlanes());
			vm.dmd.setNumberOfPlanes(vm.numberOfPlanes);
			vm.setPrevEnabled(ani!=null && vm.selectedFrame>vm.minFrame);
			vm.setNextEnabled(ani!=null && vm.selectedFrame<=vm.maxFrame);
			vm.setDrawingEnabled(nv.isMutable());
			renderAni();
		}
		vm.setMarkStartEnabled(nv!=null);
	}
	
	public void onCutInfoChanged(CutInfo ov, CutInfo nv) {
		if( nv != null ) {
			vm.setMarkEndEnabled(vm.selectedFrame > nv.start);
			vm.setCutEnabled(nv.end > nv.start);
		} else {
			vm.setMarkEndEnabled(false);
			vm.setCutEnabled(false);
		}
	}
	
	public void onMarkStart() {
		vm.setCutInfo(new CutInfo(vm.selectedFrame, 0));
	}
	
	public void onMarkEnd() {
		vm.setCutInfo(new CutInfo(vm.cutInfo.start, vm.selectedFrame));
	}
	
	public void onCutScene( CutInfo cutInfo ) {
		CompiledAnimation cutScene = vm.playingAni.cutScene(cutInfo.start, cutInfo.end, numberOfPlanes);
		
		paletteHandler.copyPalettePlaneUpgrade();
		
		String name = buildUniqueName(vm.scenes);
		cutScene.setDesc(name);
		cutScene.setPalIndex(vm.selectedPalette.index);
		cutScene.setProjectAnimation(true);
		cutScene.setEditMode(EditMode.REPLACE);
				
		model.scenes.put(name, cutScene);
		sceneHandler.populate();

		if( autoCreateKeyframe ) {
			keyFrameHandler.onAddFrameSeq(SwitchMode.REPLACE);
		}
	}
	
	/**
	 * creates a unique key name for scenes
	 * @param anis the map containing the keys
	 * @return the new unique name
	 */
	String buildUniqueName(List<CompiledAnimation> nameList) {
		int no = nameList.size();
		String name = "Scene " + no;
		while( nameList.contains(name)) {
			no++;
			name = "Scene " + no;
		}
		return name;
	}

	private void scheduleFrameInc() {
		log.debug("schedule {}", vm.delay);
		if( vm.selectedFrame<=vm.maxFrame ) {
			timerExec.exec(vm.delay+1, ()->vm.setSelectedFrame(vm.selectedFrame+1));
		} else {
			vm.setAnimationIsPlaying(false);
		}
	}
	
	public void onAnimationIsPlayingChanged(boolean ov, boolean nv) {
		vm.setStartStopLabel(vm.animationIsPlaying?"Stop":"Start");
		if( nv ) scheduleFrameInc();
	}
	
	public void onStartStop() {
		vm.setAnimationIsPlaying(!vm.animationIsPlaying);
	}
	
	public void onPrevFrame() {
		vm.setSelectedFrame(vm.selectedFrame-1);
	}
	
	public void onNextFrame() {
		vm.setSelectedFrame(vm.selectedFrame+1);
	}
	
	public void onSelectedFrameChanged(int ov, int nv) {
		if( nv != ani.actFrame ) {
			Frame f = renderAni();
			if( ani.end != vm.maxFrame ) { // sometimes end gets updated while rendering
				vm.setMaxFrame(ani.end);
			}
			vm.setDelay(ani.getRefreshDelay());
			vm.setTimecode((int) ani.getTimeCode(ani.actFrame));
			updateHashes(f);
		}
		vm.setPrevEnabled(ani!=null && vm.selectedFrame>vm.minFrame);
		vm.setNextEnabled(ani!=null && vm.selectedFrame<=vm.maxFrame);
		vm.setMarkEndEnabled(vm.cutInfo != null && vm.selectedFrame > vm.cutInfo.start);
		ani.actFrame = nv;
		if( vm.animationIsPlaying ) scheduleFrameInc();
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
			//vm.forceRedraw();
		}
		return res;
	}

}
