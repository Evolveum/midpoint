/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

/**
 * @author Radovan Semancik
 *
 */
@FunctionalInterface
public interface Handler<T> {

    // returns false if the iteration (if any) has to be stopped
    boolean handle(T t);

}
