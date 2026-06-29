/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.authorization;

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
     * Url that will be matched in {@link com.evolveum.midpoint.authentication.impl.authorization.MidPointGuiAuthorizationEvaluator}
     * using Spring Security request matcher path syntax.
     * <p/>
     * If empty then {@link Url#mountUrl()} + "/**" will be used for URL pattern matching in security configuration.
     *
     * @see {@link DescriptorLoader}
     */
    String matchUrlForSecurity() default "";
}
