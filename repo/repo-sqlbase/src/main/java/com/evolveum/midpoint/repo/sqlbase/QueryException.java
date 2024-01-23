/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import com.evolveum.midpoint.util.LocalizableMessage;

import com.google.common.base.Strings;

/**
 * Query related repository exception.
 */
public class QueryException extends RepositoryException {

    public QueryException(String message) {
        super(message);
    }

    public QueryException(LocalizableMessage localizableMessage) {
        super(localizableMessage);
    }

    public QueryException(Throwable cause) {
        super(cause);
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public static void check(boolean condition, String format, Object... args) throws QueryException {
        if (!condition) {
            throw new QueryException(Strings.lenientFormat(format, args));
        }
    }
}
