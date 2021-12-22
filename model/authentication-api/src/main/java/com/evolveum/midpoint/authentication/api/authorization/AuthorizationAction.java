/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.api.authorization;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Definition of authorization. Contains action url which define needed authorization for page containing this annotation.
 * @author lazyman
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthorizationAction {

    String label() default "";

    String description() default "";

    String actionUri();

}
