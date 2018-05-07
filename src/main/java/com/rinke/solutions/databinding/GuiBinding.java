package com.rinke.solutions.databinding;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={METHOD,FIELD})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface GuiBinding {
	/**
	 * defines which widget property to bind. Alternatively you could bind more than one, by using {@link #props()}
	 * @return
	 */
	WidgetProp prop () default WidgetProp.NONE;
	/**
	 * defines which widget properties to bind. for convenience you could also use {@link #prop()} if it is only one 
	 * @return
	 */
	WidgetProp[] props () default {};
	/**
	 * if set it overrides the standard naming convention defined in {@link WidgetProp}
	 * @return
	 */
	String propName() default "";
	
	String[] propNames () default {};
}