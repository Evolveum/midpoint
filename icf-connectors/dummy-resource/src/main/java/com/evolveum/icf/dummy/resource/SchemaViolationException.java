/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.icf.dummy.resource;

/**
 * @author semancik
 *
 */
public class SchemaViolationException extends Exception {

    public SchemaViolationException() {
        super();
    }

    public SchemaViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaViolationException(String message) {
        super(message);
    }

    public SchemaViolationException(Throwable cause) {
        super(cause);
    }

}
