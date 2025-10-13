/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Such a strange name was chosen to avoid collision with traditional @NotNull annotations.
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface NeverNull {

    boolean value() default true;
}
