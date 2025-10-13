/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
