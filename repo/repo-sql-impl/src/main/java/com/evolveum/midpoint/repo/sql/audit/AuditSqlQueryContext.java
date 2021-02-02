/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit;

import javax.xml.namespace.QName;

import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

public class AuditSqlQueryContext<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends SqlQueryContext<S, Q, R> {

    public static <S, Q extends FlexibleRelationalPathBase<R>, R> AuditSqlQueryContext<S, Q, R> from(
            Class<S> schemaType, SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {

        QueryModelMapping<S, Q, R> rootMapping = sqlRepoContext.getMappingBySchemaType(schemaType);
        Q rootPath = rootMapping.defaultAlias();
        SQLQuery<?> query = sqlRepoContext.newQuery().from(rootPath);
        // Turns on validations of aliases, does not ignore duplicate JOIN expressions,
        // we must take care of unique alias names for JOINs, which is what we want.
        query.getMetadata().setValidate(true);

        return new AuditSqlQueryContext<>(
                rootPath, rootMapping, sqlRepoContext, transformerContext, query);
    }

    private AuditSqlQueryContext(
            Q entityPath,
            QueryModelMapping<S, Q, R> mapping,
            SqlRepoContext sqlRepoContext,
            SqlTransformerContext transformerContext,
            SQLQuery<?> query) {
        super(entityPath, mapping, sqlRepoContext, transformerContext, query);
    }

    @Override
    protected SqlTransformer<S, Q, R> createTransformer() {
        return mapping.createTransformer(transformerContext, sqlRepoContext);
    }

    public <T> Class<? extends T> qNameToSchemaClass(@NotNull QName qName) {
        return transformerContext.qNameToSchemaClass(qName);
    }

    @Override
    public CanonicalItemPath createCanonicalItemPath(@NotNull ItemPath itemPath) {
        return transformerContext.prismContext().createCanonicalItemPath(itemPath);
    }

    @Override
    protected <DQ extends FlexibleRelationalPathBase<DR>, DR> SqlQueryContext<?, DQ, DR> deriveNew(
            DQ newPath, QueryModelMapping<?, DQ, DR> newMapping) {
        return new AuditSqlQueryContext<>(
                newPath, newMapping, sqlRepoContext, transformerContext, sqlQuery);
    }
}
