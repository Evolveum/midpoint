/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

/**
 * @author semancik
 *
 */
@FunctionalInterface
public interface ObjectChecker<T> {

    void check(T change);

}
