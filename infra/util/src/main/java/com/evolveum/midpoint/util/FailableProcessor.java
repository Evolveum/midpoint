/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

/**
 * @author semancik
 *
 */
@FunctionalInterface
public interface FailableProcessor<T> {

    void process(T object) throws Exception;

}
