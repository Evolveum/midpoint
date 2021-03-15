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
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

public class AuditSqlQueryContext<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends SqlQueryContext<S, Q, R> {

    public static <S, Q extends FlexibleRelationalPathBase<R>, R> AuditSqlQueryContext<S, Q, R> from(
            Class<S> schemaType, SqlTransformerSupport transformerSupport, SqlRepoContext sqlRepoContext) {

        QueryTableMapping<S, Q, R> rootMapping = sqlRepoContext.getMappingBySchemaType(schemaType);
        Q rootPath = rootMapping.defaultAlias();
        SQLQuery<?> query = sqlRepoContext.newQuery().from(rootPath);
        // Turns on validations of aliases, does not ignore duplicate JOIN expressions,
        // we must take care of unique alias names for JOINs, which is what we want.
        query.getMetadata().setValidate(true);

        return new AuditSqlQueryContext<>(
                rootPath, rootMapping, sqlRepoContext, transformerSupport, query);
    }

    private AuditSqlQueryContext(
            Q entityPath,
            QueryTableMapping<S, Q, R> mapping,
            SqlRepoContext sqlRepoContext,
            SqlTransformerSupport transformerSupport,
            SQLQuery<?> query) {
        super(entityPath, mapping, sqlRepoContext, transformerSupport, query);
    }

    @Override
    protected SqlTransformer<S, Q, R> createTransformer() {
        return entityPathMapping.createTransformer(transformerSupport);
    }

    @Override
    protected <DQ extends FlexibleRelationalPathBase<DR>, DR> SqlQueryContext<?, DQ, DR> deriveNew(
            DQ newPath, QueryTableMapping<?, DQ, DR> newMapping) {
        return new AuditSqlQueryContext<>(
                newPath, newMapping, sqlRepoContext, transformerSupport, sqlQuery);
    }
}
