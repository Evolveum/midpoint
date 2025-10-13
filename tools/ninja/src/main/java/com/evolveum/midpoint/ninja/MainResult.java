/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja;

/**
 * Class that allows to return result and expected exit code from ninja execution.
 * <p>
 * It's not a record for now because of the need to support Java 11.
 */
public final class MainResult<T> {

    public static final int DEFAULT_EXIT_CODE_ERROR = 1;

    public static final int DEFAULT_EXIT_CODE_SUCCESS = 0;

    private final T result;

    private final int exitCode;

    private final String exitMessage;

    public static final MainResult<?> EMPTY_ERROR = new MainResult<>(null, DEFAULT_EXIT_CODE_ERROR);

    public static final MainResult<?> EMPTY_SUCCESS = new MainResult<>(null, DEFAULT_EXIT_CODE_SUCCESS);

    public MainResult(T result) {
        this(result, 0);
    }

    public MainResult(T result, int exitCode) {
        this(result, exitCode, null);
    }

    public MainResult(T result, int exitCode, String exitMessage) {
        this.result = result;
        this.exitCode = exitCode;
        this.exitMessage = exitMessage;
    }

    public T result() {
        return result;
    }

    public int exitCode() {
        return exitCode;
    }

    public String exitMessage() {
        return exitMessage;
    }
}
