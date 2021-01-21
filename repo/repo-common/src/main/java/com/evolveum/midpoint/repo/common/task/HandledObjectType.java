/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Object type that is processed by given task part.
 * It is used "as is", no other source of this information is considered.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Experimental
public @interface HandledObjectType {

    Class<? extends ObjectType> value();

}
