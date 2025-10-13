/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by Viliam Repan (lazyman).
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface JaxbPaths {

    JaxbPath[] value();
}
