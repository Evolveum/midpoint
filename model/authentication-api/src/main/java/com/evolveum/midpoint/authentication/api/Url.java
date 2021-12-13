/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An interface used for definition page url in GUI.
 *
 * @author lazyman
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Url {

    /**
     * Url belonging to the given Page.
     */
    String mountUrl();

    /**
     * If empty {@link Url#mountUrl()} + "/**" will be used for URL ant pattern matching in security configuration.
     * See {@link DescriptorLoader}, {@link com.evolveum.midpoint.authentication.impl.authorization.MidPointGuiAuthorizationEvaluator}.
     */
    String matchUrlForSecurity() default "";
}
