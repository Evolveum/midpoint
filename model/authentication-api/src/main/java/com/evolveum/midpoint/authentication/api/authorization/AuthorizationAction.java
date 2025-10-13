/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
