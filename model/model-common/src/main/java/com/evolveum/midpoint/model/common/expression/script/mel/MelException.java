/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel;

public class MelException extends RuntimeException {

    public MelException(String message) {
        super(message);
    }

    public MelException(String message, Throwable cause) {
        super(message, cause);
    }

}
