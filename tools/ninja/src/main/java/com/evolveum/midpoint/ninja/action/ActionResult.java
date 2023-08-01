/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action;

/**
 * Class that allows to return result and expected exit code from action execution.
 * <p>
 * It's not a record for now because of the need to support Java 11.
 */
public final class ActionResult<T> {

    private final T result;

    private final int exitCode;

    public ActionResult(T result) {
        this(result, 0);
    }

    public ActionResult(T result, int exitCode) {
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
