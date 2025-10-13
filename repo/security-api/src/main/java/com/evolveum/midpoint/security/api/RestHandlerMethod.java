/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
