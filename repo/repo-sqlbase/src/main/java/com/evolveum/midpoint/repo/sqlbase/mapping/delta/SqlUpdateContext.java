/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping.delta;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * TODO
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class SqlUpdateContext<S extends Objectable, Q extends FlexibleRelationalPathBase<R>, R> {

    protected final QueryTableMapping<S, Q, R> mapping;
    protected final JdbcSession jdbcSession;
    protected final PrismObject<S> prismObject;
    protected final Q rootPath;

    public SqlUpdateContext(
            QueryTableMapping<S, Q, R> mapping,
            JdbcSession jdbcSession,
            PrismObject<S> prismObject) {
        this.mapping = mapping;
        this.jdbcSession = jdbcSession;
        this.prismObject = prismObject;
        rootPath = mapping.defaultAlias();
    }
}
