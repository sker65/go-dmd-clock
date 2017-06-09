package com.rinke.solutions.databinding;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={METHOD,FIELD})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface PojoBinding {
	/**
	 * defines which src pojo property of the annotated field to bind. 
	 */
	String src ();
	
	String[] srcs () default {};
	/**
	 * defines the name of the target property in the view model
	 */
	String target() default "";
	
	String[] targets () default {};

}
