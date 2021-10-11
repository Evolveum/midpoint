/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query;

/**
 * @author lazyman
 */
public class QueryException extends Exception {

    public QueryException(String s) {
        super(s);
    }

    public QueryException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
