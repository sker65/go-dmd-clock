package com.rinke.solutions.databinding;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for the databinder to make sure / create a property change listener
 * @author stefan rinke
 */

@Target(value={TYPE,METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface PropertyBound {
	Class<?> target () default Object.class;
}
