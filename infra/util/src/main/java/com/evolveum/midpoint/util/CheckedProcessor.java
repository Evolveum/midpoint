/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

@FunctionalInterface
public interface CheckedProcessor<T,E extends Exception> {

    void process(T value) throws E;
}
