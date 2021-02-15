/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.repo.sqale.qmodel.SqaleModelMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

// TODO change the parametric bounds together with change for SqaleObjectMapping
public class SqaleQueryContext<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends SqlQueryContext<S, Q, R> {

    public static <S, Q extends FlexibleRelationalPathBase<R>, R> SqaleQueryContext<S, Q, R> from(
            Class<S> schemaType, SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {

        SqaleModelMapping<S, Q, R> rootMapping = sqlRepoContext.getMappingBySchemaType(schemaType);
        Q rootPath = rootMapping.defaultAlias();
        SQLQuery<?> query = sqlRepoContext.newQuery().from(rootPath);
        // Turns on validations of aliases, does not ignore duplicate JOIN expressions,
        // we must take care of unique alias names for JOINs, which is what we want.
        query.getMetadata().setValidate(true);

        return new SqaleQueryContext<>(rootPath, rootMapping, transformerContext, sqlRepoContext, query);
    }

    private SqaleQueryContext(
            Q entityPath,
            SqaleModelMapping<S, Q, R> mapping,
            SqlTransformerContext transformerContext,
            SqlRepoContext sqlRepoContext,
            SQLQuery<?> query) {
        super(entityPath, mapping, sqlRepoContext, transformerContext, query);
    }

    @Override
    protected SqlTransformer<S, Q, R> createTransformer() {
        return entityPathMapping.createTransformer(transformerContext);
    }

    /**
     * Returns {@link SqaleQueryContext} - lot of ugly casting here, but it is not possible to
     * use covariant return type with covariant parametric types (narrower generics).
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected <DQ extends FlexibleRelationalPathBase<DR>, DR> SqlQueryContext<?, DQ, DR>
    deriveNew(DQ newPath, QueryTableMapping<?, DQ, DR> newMapping) {
        return (SqlQueryContext<?, DQ, DR>) new SqaleQueryContext(
                newPath, (SqaleModelMapping<?, ?, ?>) newMapping,
                transformerContext, sqlRepoContext, sqlQuery);
    }
}
