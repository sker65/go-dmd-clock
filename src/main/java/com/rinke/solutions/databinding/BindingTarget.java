package com.rinke.solutions.databinding;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * marks a field in a view model as binding target (a candidate). The data binder, that processes these type of annotation
 * tries to find a widget (and property) that could be bound to that annotated property 
 * @author stefanri
 *
 */
@Target(value={TYPE,METHOD,FIELD})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface BindingTarget {

}
