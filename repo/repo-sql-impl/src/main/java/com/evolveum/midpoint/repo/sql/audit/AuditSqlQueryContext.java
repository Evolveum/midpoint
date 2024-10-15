/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit;

import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * @param <S> schema type, used by encapsulated mapping
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class AuditSqlQueryContext<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends SqlQueryContext<S, Q, R> {

    // Type parameters the same as in the class documentation.
    public static <S, Q extends FlexibleRelationalPathBase<R>, R> AuditSqlQueryContext<S, Q, R>
    from(Class<S> schemaType, SqlRepoContext sqlRepoContext) {
        QueryTableMapping<S, Q, R> rootMapping = sqlRepoContext.getMappingBySchemaType(schemaType);
        Q rootPath = rootMapping.defaultAlias();
        SQLQuery<?> query = sqlRepoContext.newQuery().from(rootPath);
        // Turns on validations of aliases, does not ignore duplicate JOIN expressions,
        // we must take care of unique alias names for JOINs, which is what we want.
        query.getMetadata().setValidate(true);

        return new AuditSqlQueryContext<>(
                rootPath, rootMapping, sqlRepoContext, query);
    }

    private AuditSqlQueryContext(
            Q entityPath,
            QueryTableMapping<S, Q, R> mapping,
            SqlRepoContext sqlRepoContext,
            SQLQuery<?> query) {
        super(entityPath, mapping, sqlRepoContext, query);
    }

    private AuditSqlQueryContext(
            Q entityPath,
            QueryTableMapping<S, Q, R> mapping,
            AuditSqlQueryContext<?, ?, ?> parentContext,
            SQLQuery<?> sqlQuery) {
        super(entityPath, mapping, parentContext, sqlQuery);
    }

    @Override
    public <TS, TQ extends FlexibleRelationalPathBase<TR>, TR>
    SqlQueryContext<TS, TQ, TR> newSubcontext(
            TQ newPath, QueryTableMapping<TS, TQ, TR> newMapping) {
        return new AuditSqlQueryContext<>(newPath, newMapping, this, this.sqlQuery);
    }

    @Override
    protected <TS, TQ extends FlexibleRelationalPathBase<TR>, TR>
    SqlQueryContext<TS, TQ, TR> newSubcontext(
            TQ newPath, QueryTableMapping<TS, TQ, TR> newMapping, SQLQuery<?> query) {
        return new AuditSqlQueryContext<>(newPath, newMapping, this, query);
    }
}
