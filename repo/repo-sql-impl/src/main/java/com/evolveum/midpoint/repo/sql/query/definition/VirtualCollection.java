/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lazyman
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface VirtualCollection {

    JaxbName jaxbName();

    Class<?> jaxbType();

    String jpaName();

    Class<?> jpaType();

    VirtualQueryParam[] additionalParams() default {};

    Class<?> collectionType();
}
