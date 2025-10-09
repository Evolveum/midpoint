/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author lazyman
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface JaxbName {

    String namespace() default SchemaConstantsGenerated.NS_COMMON;

    String localPart();
}
