/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Used to check whether particular code was or was not run, e.g. when testing execution profiles.
 */
public class RunFlag {

    private boolean value;

    public void set() {
        value = true;
    }

    public void reset() {
        value = false;
    }

    public void assertSet() {
        assertThat(value).as("'set' was run").isTrue();
    }

    public void assertNotSet() {
        assertThat(value).as("'set' was run").isFalse();
    }

    @Override
    public String toString() {
        return "RunFlag{" +
                "value=" + value +
                '}';
    }
}
