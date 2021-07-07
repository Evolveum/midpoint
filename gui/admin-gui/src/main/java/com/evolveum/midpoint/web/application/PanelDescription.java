package com.evolveum.midpoint.web.application;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PanelDescription {

    String identifier() default "";

}
