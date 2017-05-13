package com.rinke.solutions.beans;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;

@Target(value={TYPE,METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {
	Scope scope() default Scope.SINGLETON;
	String name() default "";
}