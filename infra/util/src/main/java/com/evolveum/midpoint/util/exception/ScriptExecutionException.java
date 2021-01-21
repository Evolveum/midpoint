/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.exception;

/**
 * Wraps any exceptions that occur during execution of expressions. (ExpressionEvaluationException would be more appropriate, but this name is already used elsewhere.)
 */
public class ScriptExecutionException extends CommonException {

    public ScriptExecutionException() {
    }

    public ScriptExecutionException(String message) {
        super(message);
    }

    public ScriptExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorTypeMessage() {
        return "Script execution exception";
    }
}
