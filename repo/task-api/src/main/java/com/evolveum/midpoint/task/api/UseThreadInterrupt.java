/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

public enum UseThreadInterrupt {

    NEVER("never"), WHEN_NECESSARY("whenNecessary"), ALWAYS("always");

    private final String value;

    UseThreadInterrupt(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static UseThreadInterrupt fromValue(String v) {
        for (UseThreadInterrupt c: UseThreadInterrupt.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
