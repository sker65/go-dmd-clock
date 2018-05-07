package com.rinke.solutions.beans;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;

@Target(value={CONSTRUCTOR,METHOD,PARAMETER,FIELD,ANNOTATION_TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
	String defaultValue() default "";
	String key() default "";
}