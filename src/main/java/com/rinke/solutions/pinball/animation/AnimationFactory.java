package com.rinke.solutions.pinball.animation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.rinke.solutions.pinball.renderer.Renderer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * class that creates a list of animations read from a property file
 * @author sr
 *
 */
public class AnimationFactory {
	
    private static final String GETAWAY = "Getaway";
    
    private static AnimationCompiler animationCompiler = new AnimationCompiler();

	public static List<Animation> createAnimationsFromProperties(String filename) {
	    File ifile = new File(filename);
	    Config conf = ConfigFactory.parseFileAnySyntax(ifile);
		//Config conf = ConfigFactory.parseResourcesAnySyntax(filename);
		List<Animation> result = new ArrayList<>();
		String basePath = ifile.getParent();
		if( !basePath.endsWith("/")) basePath += "/";
		
		// load compiled file if any
		if( conf.hasPath("compiled")) {
			String file = conf.getString("compiled");
			result.addAll(animationCompiler.readFromCompiledFile(file));
		}
		
		if( conf.hasPath("base")) {
			basePath = conf.getString("base");
		}

		for(String animationName : conf.root().keySet() ) {
			AnimationType type = AnimationType.PNG;
			if( animationName.equals("compiled") || animationName.equals("base")) continue;
			
			if( conf.hasPath(animationName+".type") ) {
				type = typeFor(conf.getString(animationName+".type"));
			}
			int cycles = 1;
			if( conf.hasPath(animationName+".cycles") ) {
				cycles = conf.getInt(animationName+".cycles");
			}
			Animation animation = new Animation(
					type,
					conf.getString(animationName+".path"),
					getInt(conf,animationName+".start"),
					getInt(conf,animationName+".end"),
					conf.getInt(animationName+".step"),
					cycles,
					conf.getInt(animationName+".hold"));
			
			animation.setDesc(animationName);
			animation.setBasePath(basePath);
			
			if( conf.hasPath(animationName+".millisPerCycle")) {
				animation.setRefreshDelay(conf.getInt(animationName+".millisPerCycle"));
			}
			if( conf.hasPath(animationName+".autoMerge")) {
				animation.setAutoMerge(conf.getBoolean(animationName+".autoMerge"));
			}

			if( conf.hasPath(animationName+".clockFrom")) {
				animation.setClockFrom(getInt(conf,animationName+".clockFrom"));
			}
			if( conf.hasPath(animationName+".clockSmall")) {
				animation.setClockSmall(conf.getBoolean(animationName+".clockSmall"));
			}
			if( conf.hasPath(animationName+".fsk")) {
				animation.setFsk(getInt(conf,animationName+".fsk"));
			}
			if( conf.hasPath(animationName+".clockInFront")) {
				animation.setClockInFront(conf.getBoolean(animationName+".clockInFront"));
			} else {
				if(animation.getClockFrom()<10000) animation.setClockInFront(true); // in front is default
			}
			if( conf.hasPath(animationName+".clockXOffset")) {
				animation.setClockXOffset(conf.getInt(animationName+".clockXOffset"));
			}
			if( conf.hasPath(animationName+".clockYOffset")) {
				animation.setClockYOffset(conf.getInt(animationName+".clockYOffset"));
			}
			
			if( conf.hasPath(animationName+".pattern")) {
				animation.setPattern(conf.getString(animationName+".pattern"));
			}	
			
			if( conf.hasPath(animationName+".thresholds") ) {
				List<Integer> thresholds = conf.getIntList(animationName+".thresholds");
				Renderer r =  animation.getRenderer();
				r.setLowThreshold(thresholds.get(0));
				r.setMidThreshold(thresholds.get(1));
				r.setHighThreshold(thresholds.get(2));
			}
			
			if( conf.hasPath(animationName+".transitionFrom") ) {
				animation.setTransitionFrom(getInt(conf,animationName+".transitionFrom"));
			}
			if( conf.hasPath(animationName+".transitionDelay") ) {
				animation.setTransitionDelay(getInt(conf,animationName+".transitionDelay"));
			}
			
			if( conf.hasPath(animationName+".transitionName") ) {
				animation.setTransitionName(conf.getString(animationName+".transitionName"));
			}
			
			result.add(animation);
		}
		return result;
	}

	private static int getInt(Config conf, String key) {
		String v = conf.getString(key);
		if( v.startsWith("0x")) {
			return Integer.parseInt(v.substring(2), 16);
		} else {
			return Integer.parseInt(v);
		}
	}

	private static AnimationType typeFor(String type) {
		if(type==null) return AnimationType.PNG;
		return AnimationType.valueOf(type);
	}

}
