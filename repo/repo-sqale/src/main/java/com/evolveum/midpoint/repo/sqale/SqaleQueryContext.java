/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sqale.filtering.ExistsFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.InOidFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.OrgFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.TypeFilterProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

public class SqaleQueryContext<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends SqlQueryContext<S, Q, R> {

    private boolean containsOrgFilter = false;

    public static <S, Q extends FlexibleRelationalPathBase<R>, R> SqaleQueryContext<S, Q, R> from(
            Class<S> schemaType,
            SqaleRepoContext sqlRepoContext) {

        SqaleTableMapping<S, Q, R> rootMapping = sqlRepoContext.getMappingBySchemaType(schemaType);
        Q rootPath = rootMapping.defaultAlias();
        SQLQuery<?> query = sqlRepoContext.newQuery().from(rootPath);
        // Turns on validations of aliases, does not ignore duplicate JOIN expressions,
        // we must take care of unique alias names for JOINs, which is what we want.
        query.getMetadata().setValidate(true);

        return new SqaleQueryContext<>(
                rootPath, rootMapping, sqlRepoContext, query);
    }

    private SqaleQueryContext(
            Q entityPath,
            SqaleTableMapping<S, Q, R> mapping,
            SqaleRepoContext sqlRepoContext,
            SQLQuery<?> query) {
        super(entityPath, mapping, sqlRepoContext, query);
    }

    private SqaleQueryContext(
            Q entityPath,
            SqaleTableMapping<S, Q, R> mapping,
            SqaleQueryContext<?, ?, ?> parentContext,
            SQLQuery<?> sqlQuery) {
        super(entityPath, mapping, parentContext, sqlQuery);
    }

    @Override
    public Predicate process(@NotNull ObjectFilter filter) throws RepositoryException {
        // To compare with old repo see: QueryInterpreter.findAndCreateRestrictionInternal
        if (filter instanceof InOidFilter) {
            return new InOidFilterProcessor(this).process((InOidFilter) filter);
        } else if (filter instanceof OrgFilter) {
            return new OrgFilterProcessor(this).process((OrgFilter) filter);
        } else if (filter instanceof FullTextFilter) {
            // TODO
            throw new QueryException("TODO filter " + filter);
        } else if (filter instanceof ExistsFilter) {
            return new ExistsFilterProcessor<>(this).process((ExistsFilter) filter);
        } else if (filter instanceof TypeFilter) {
            return new TypeFilterProcessor<>(this).process((TypeFilter) filter);
        } else {
            return super.process(filter);
        }
    }

    @Override
    public SqaleRepoContext repositoryContext() {
        return (SqaleRepoContext) super.repositoryContext();
    }

    public @NotNull Integer searchCachedRelationId(QName qName) {
        return repositoryContext().searchCachedRelationId(qName);
    }

    public void markContainsOrgFilter() {
        containsOrgFilter = true;
        SqaleQueryContext<?, ?, ?> parentContext = parentContext();
        if (parentContext != null) {
            parentContext.markContainsOrgFilter();
        }
    }

    /** Returns derived {@link SqaleQueryContext} for JOIN. */
    @Override
    protected <TS, TQ extends FlexibleRelationalPathBase<TR>, TR> SqlQueryContext<TS, TQ, TR>
    newSubcontext(TQ newPath, QueryTableMapping<TS, TQ, TR> newMapping) {
        return new SqaleQueryContext<>(
                newPath,
                (SqaleTableMapping<TS, TQ, TR>) newMapping,
                this,
                this.sqlQuery);
    }

    /** Returns derived {@link SqaleQueryContext} for subquery. */
    @Override
    protected <TS, TQ extends FlexibleRelationalPathBase<TR>, TR> SqlQueryContext<TS, TQ, TR>
    newSubcontext(TQ newPath, QueryTableMapping<TS, TQ, TR> newMapping, SQLQuery<?> query) {
        return new SqaleQueryContext<>(
                newPath,
                (SqaleTableMapping<TS, TQ, TR>) newMapping,
                this,
                query);
    }

    @Override
    public SqaleQueryContext<?, ?, ?> parentContext() {
        return (SqaleQueryContext<?, ?, ?>) super.parentContext();
    }

    @Override
    public void beforeQuery() {
        if (containsOrgFilter) {
            try (JdbcSession jdbcSession = repositoryContext().newJdbcSession().startTransaction()) {
                jdbcSession.executeStatement("CALL m_refresh_org_closure()");
                jdbcSession.commit();
            }
        }
    }
}
