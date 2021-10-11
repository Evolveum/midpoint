/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author lazyman
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Url {

    String mountUrl();

    /**
     * If empty {@link Url#mountUrl()} + "/**" will be used for URL ant pattern matching in security configuration.
     * See {@link DescriptorLoader}, {@link com.evolveum.midpoint.web.security.MidPointGuiAuthorizationEvaluator}.
     */
    String matchUrlForSecurity() default "";
}
