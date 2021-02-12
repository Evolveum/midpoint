/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

public class RepositoryServiceFactoryException extends Exception {

    private static final long serialVersionUID = -5462545970628220734L;

    public RepositoryServiceFactoryException(String message) {
        super(message);
    }

    public RepositoryServiceFactoryException(Throwable cause) {
        super(cause);
    }

    public RepositoryServiceFactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
