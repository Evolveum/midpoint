/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
