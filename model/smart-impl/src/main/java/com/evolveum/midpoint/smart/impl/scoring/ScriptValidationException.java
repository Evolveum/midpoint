/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.scoring;

/**
 * Exception thrown when mapping script validation fails during execution.
 * This indicates that a suggested mapping script cannot be executed successfully.
 */
public class ScriptValidationException extends Exception {

    public ScriptValidationException(String message) {
        super(message);
    }

}
