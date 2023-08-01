/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

/**
 * Class that allows to return result and expected exit code from ninja execution.
 * <p>
 * It's not a record for now because of the need to support Java 11.
 */
public final class MainResult<T> {

    private final T result;

    private final int exitCode;

    public static final MainResult<?> EMPTY_ERROR = new MainResult<>(null, 1);

    public static final MainResult<?> EMPTY_SUCCESS = new MainResult<>(null, 0);

    public MainResult(T result) {
        this(result, 0);
    }

    public MainResult(T result, int exitCode) {
        this.result = result;
        this.exitCode = exitCode;
    }

    public T result() {
        return result;
    }

    public int exitCode() {
        return exitCode;
    }
}
