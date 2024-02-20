/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.api;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * MidPoint-specific information about a REST handler method. Currently, it provides related {@link RestAuthorizationAction}.
 */
@Target(ElementType.METHOD)
@Retention(RUNTIME)
public @interface RestHandlerMethod {

    /** The abstract REST method implemented by this handler. */
    @NotNull RestAuthorizationAction authorization();
}
