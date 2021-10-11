/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
