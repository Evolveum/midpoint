/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.jsonb;

/**
 * Technical exception capturing problems with reading or writing the JSONB value.
 * This should never occur as a business exception, it's a result of application/programming error.
 */
public class JsonbException extends RuntimeException {

    public JsonbException(String message, Throwable cause) {
        super(message, cause);
    }
}
