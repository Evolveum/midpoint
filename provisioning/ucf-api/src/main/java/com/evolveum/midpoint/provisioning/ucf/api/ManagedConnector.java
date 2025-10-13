/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author Radovan Semancik
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface ManagedConnector {

    String type() default "";
    String version() default "1.0.0";

}
