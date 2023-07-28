/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

public class MainResult {

    public static final MainResult EMPTY_ERROR = new MainResult(null, 1);

    public static final MainResult EMPTY_SUCCESS = new MainResult(null, 0);

    private Object object;

    private int exitCode;

    public MainResult(Object object) {
        this(object, 0);
    }

    public MainResult(Object object, int exitCode) {
        this.object = object;
        this.exitCode = exitCode;
    }

    public Object getObject() {
        return object;
    }

    public int getExitCode() {
        return exitCode;
    }
}
