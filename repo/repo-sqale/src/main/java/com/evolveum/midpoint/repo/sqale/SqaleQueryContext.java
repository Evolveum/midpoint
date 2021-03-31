/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

public class SqaleQueryContext<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends SqlQueryContext<S, Q, R> {

    public static <S, Q extends FlexibleRelationalPathBase<R>, R> SqaleQueryContext<S, Q, R> from(
            Class<S> schemaType, SqlTransformerSupport transformerSupport, SqlRepoContext sqlRepoContext) {

        SqaleTableMapping<S, Q, R> rootMapping = sqlRepoContext.getMappingBySchemaType(schemaType);
        Q rootPath = rootMapping.defaultAlias();
        SQLQuery<?> query = sqlRepoContext.newQuery().from(rootPath);
        // Turns on validations of aliases, does not ignore duplicate JOIN expressions,
        // we must take care of unique alias names for JOINs, which is what we want.
        query.getMetadata().setValidate(true);

        return new SqaleQueryContext<>(rootPath, rootMapping, transformerSupport, sqlRepoContext, query);
    }

    private SqaleQueryContext(
            Q entityPath,
            SqaleTableMapping<S, Q, R> mapping,
            SqlTransformerSupport transformerSupport,
            SqlRepoContext sqlRepoContext,
            SQLQuery<?> query) {
        super(entityPath, mapping, sqlRepoContext, transformerSupport, query);
    }

    @Override
    protected SqlTransformer<S, Q, R> createTransformer() {
        return entityPathMapping.createTransformer(transformerSupport);
    }

    @Override
    public FilterProcessor<InOidFilter> createInOidFilter(SqlQueryContext<?, ?, ?> context) {
        return new InOidFilterProcessor(context);
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
                newPath, (SqaleTableMapping<?, ?, ?>) newMapping,
                transformerSupport, sqlRepoContext, sqlQuery);
    }
}
