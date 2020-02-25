/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

/**
 *  Experimental.
 */
@Deprecated
public class ImmutableUtil {

    public static void throwImmutable(Object object) {
        throw new IllegalStateException("Object " + object + " couldn't be modified as it is immutable");
    }
}
