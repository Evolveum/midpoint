/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;

import java.io.Serializable;

/**
 * Almost the same as Consumer but this one is Serializable and can throw CommonException.
 * EXPERIMENTAL
 */
@Experimental
@FunctionalInterface
public interface CheckedConsumer<T> extends Serializable {

    void accept(T argument) throws CommonException;
}
