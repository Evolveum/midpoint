/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lazyman
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryEntity {

    JaxbName jaxbName() default @JaxbName(localPart = "");

    Class jaxbType() default Object.class;

    VirtualProperty[] properties() default {};

    VirtualCollection[] collections() default {};

    VirtualEntity[] entities() default {};

    VirtualAny[] anyElements() default {};

    VirtualReference[] references() default {};
}
