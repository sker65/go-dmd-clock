package com.rinke.solutions.databinding;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={TYPE,METHOD,FIELD})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface Command {

}