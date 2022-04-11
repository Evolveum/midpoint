package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.prism.Containerable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PanelType {

    String name() default "";

    boolean generic() default false;

    String defaultContainerPath() default "";

    boolean experimental() default false;

    Class<? extends Containerable> defaultType() default Containerable.class;
}
