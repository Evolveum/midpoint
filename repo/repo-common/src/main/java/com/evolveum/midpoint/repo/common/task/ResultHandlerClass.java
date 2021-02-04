/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.util.annotation.Experimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies result handler implementation class for given task part execution class.
 *
 * Requirements:
 * 1. The class and its constructor must be public.
 * 2. ... (TODO)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Experimental
public @interface ResultHandlerClass {

    Class<? extends AbstractSearchIterativeResultHandler<?, ?, ?, ?, ?>> value();

}
