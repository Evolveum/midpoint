/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

/**
 * Runtime exception wrapping other exception that occurred during object transformation
 * inside mapping (e.g. tuple to schema object).
 */
public class RepositoryMappingException extends RuntimeException {

    public RepositoryMappingException(Throwable cause) {
        super(cause);
    }

    public RepositoryMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
