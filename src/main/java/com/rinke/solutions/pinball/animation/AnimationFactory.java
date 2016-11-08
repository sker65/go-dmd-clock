package com.rinke.solutions.pinball.animation;

import java.io.File;

import static java.lang.Boolean.parseBoolean;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import com.rinke.solutions.pinball.renderer.Renderer;

/**
 * class that creates a list of animations read from a property file
 * @author sr
 *
 */
public class AnimationFactory {
	
    private static AnimationCompiler animationCompiler = new AnimationCompiler();

	public static List<Animation> createAnimationsFromProperties(String filename) throws IOException {
		
		Properties conf = new Properties();
		conf.load(new FileInputStream(filename));
		List<Animation> result = new ArrayList<>();

		String basePath = new File(filename).getParent();
		if( !basePath.endsWith("/")) basePath += "/";
		
		// load compiled file if any
		if( conf.containsKey("compiled")) {
			String file = conf.getProperty("compiled");
			result.addAll(animationCompiler.readFromCompiledFile(file));
		}
		
		if( conf.containsKey("base")) {
			basePath = conf.getProperty("base");
			if( !basePath.endsWith("/")) basePath += "/";
		}
		
		List<String> aniNames = createAninames(conf);

		for(String animationName : aniNames ) {
			AnimationType type = AnimationType.PNG;
						
			if( animationName.equals("compiled") || animationName.equals("base")) continue;
			
			if( conf.containsKey(animationName+".type") ) {
				type = typeFor(conf.getProperty(animationName+".type"));
			}
			int cycles = 1;
			if( conf.containsKey(animationName+".cycles") ) {
				cycles = getInt(conf, animationName+".cycles");
			}
			Animation animation = new Animation(
					type,
					conf.getProperty(animationName+".path"),
					getInt(conf,animationName+".start"),
					getInt(conf,animationName+".end"),
					getInt(conf, animationName+".step",1),
					cycles,
					getInt(conf,animationName+".hold",1));
			
			animation.setDesc(animationName);
			animation.setBasePath(basePath);
			
			animation.setProps(createPropsForAniname(conf, animationName));
			animation.setMutable(Boolean.parseBoolean(conf.getProperty(animationName+".mutable", "false")));
			
			if( conf.containsKey(animationName+".millisPerCycle")) {
				animation.setRefreshDelay(getInt(conf, animationName+".millisPerCycle"));
			}
			if( conf.containsKey(animationName+".autoMerge")) {
				animation.setAutoMerge(parseBoolean(conf.getProperty(animationName+".autoMerge")));
			}

			if( conf.containsKey(animationName+".clockFrom")) {
				animation.setClockFrom(getInt(conf,animationName+".clockFrom"));
			}
			if( conf.containsKey(animationName+".clockSmall")) {
				animation.setClockSmall(parseBoolean(conf.getProperty(animationName+".clockSmall")));
			}
			if( conf.containsKey(animationName+".fsk")) {
				animation.setFsk(getInt(conf,animationName+".fsk"));
			}
			if( conf.containsKey(animationName+".clockInFront")) {
				animation.setClockInFront(parseBoolean(conf.getProperty(animationName+".clockInFront")));
			} else {
				if(animation.getClockFrom()<10000) animation.setClockInFront(true); // in front is default
			}
			if( conf.containsKey(animationName+".clockXOffset")) {
				animation.setClockXOffset(getInt(conf, animationName+".clockXOffset"));
			}
			if( conf.containsKey(animationName+".clockYOffset")) {
				animation.setClockYOffset(getInt(conf, animationName+".clockYOffset"));
			}
			
			if( conf.containsKey(animationName+".pattern")) {
				animation.setPattern(conf.getProperty(animationName+".pattern"));
			}	
			
			if( conf.containsKey(animationName+".thresholds") ) {
				List<Integer> thresholds = getIntList(conf,animationName+".thresholds");
				Renderer r =  animation.getRenderer();
				r.setLowThreshold(thresholds.get(0));
				r.setMidThreshold(thresholds.get(1));
				r.setHighThreshold(thresholds.get(2));
			}
			
			if( conf.containsKey(animationName+".transitionFrom") ) {
				animation.setTransitionFrom(getInt(conf,animationName+".transitionFrom"));
			}
			if( conf.containsKey(animationName+".transitionDelay") ) {
				animation.setTransitionDelay(getInt(conf,animationName+".transitionDelay"));
			}
			
			if( conf.containsKey(animationName+".transitionName") ) {
				animation.setTransitionName(conf.getProperty(animationName+".transitionName"));
			}
			
			result.add(animation);
		}
		return result;
	}

	private static Properties createPropsForAniname(Properties conf, String animationName) {
		Properties properties = new Properties();
		for(Object key : conf.keySet() ) {
			String k = (String)key;
			if( k.startsWith(animationName)) {
				properties.setProperty(k.substring(animationName.length()+1), conf.getProperty(k));
			}
		}
		return properties;
	}

	private static List<Integer> getIntList(Properties conf, String prefix) {
		int n = 0;
		String key = prefix + "." + n;
		List<Integer> res = new ArrayList<Integer>();
		while( conf.containsKey(key)) {
			res.add(Integer.parseInt(conf.getProperty(key)));
			n++;
			key = prefix + "." + n;
		}
		return res ;
	}

	private static List<String> createAninames(Properties conf) {
		HashSet<String> res = new HashSet<>();
		for(Object key : conf.keySet() ) {
			String k = (String)key;
			if( k.contains(".")) {
				String ani = k.substring(0,k.indexOf('.'));
				res.add(ani);
			}
		}
		return new ArrayList<String>(res);
	}

	private static int getInt(Properties conf, String key) {
		String v = conf.getProperty(key);
		if( v.startsWith("0x")) {
			return Integer.parseInt(v.substring(2), 16);
		} else {
			return Integer.parseInt(v);
		}
	}
	
	private static int getInt(Properties conf, String key, int defValue) {
		if( conf.contains(key) ) return getInt(conf,key);
		return defValue;
	}

	private static AnimationType typeFor(String type) {
		if(type==null) return AnimationType.PNG;
		return AnimationType.valueOf(type);
	}

}
