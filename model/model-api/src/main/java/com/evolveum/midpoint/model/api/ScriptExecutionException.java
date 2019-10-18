/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

/**
 * Wraps any exceptions that occur during execution of expressions. (ExpressionEvaluationException would be more appropriate, but this name is already used elsewhere.)
 *
 * @author mederly
 */
public class ScriptExecutionException extends Exception {

    public ScriptExecutionException() {
    }

    public ScriptExecutionException(String message) {
        super(message);
    }

    public ScriptExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
