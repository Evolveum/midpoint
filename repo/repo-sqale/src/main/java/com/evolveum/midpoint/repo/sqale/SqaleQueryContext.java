/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.Collection;
import java.util.UUID;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.FuzzyStringMatchFilter.FuzzyMatchingMethod;
import com.evolveum.midpoint.prism.query.FuzzyStringMatchFilter.Levenshtein;
import com.evolveum.midpoint.prism.query.FuzzyStringMatchFilter.Similarity;
import com.evolveum.midpoint.repo.sqale.filtering.*;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleNestedMapping;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

public class SqaleQueryContext<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends SqlQueryContext<S, Q, R> {

    /**
     * Flag guarding whether we need to refresh organization closure before executing
     * the actual query.
     */
    private boolean containsOrgFilter = false;

    /**
     * Enables {@link #loadObject} method that is used to fetch additional objects,
     * e.g. container owners (parents) or references targets.
     */
    private final SqaleObjectLoader objectLoader;

    private final QueryModelMapping<S, Q, R> queryMapping;

    public static <S, Q extends FlexibleRelationalPathBase<R>, R> SqaleQueryContext<S, Q, R> from(
            Class<S> schemaType,
            SqaleRepoContext sqlRepoContext) {
        return from(schemaType, sqlRepoContext, null);
    }

    public static <S, Q extends FlexibleRelationalPathBase<R>, R> SqaleQueryContext<S, Q, R> from(
            Class<S> schemaType,
            SqaleRepoContext sqlRepoContext,
            SqaleObjectLoader objectLoader) {
        return from(schemaType, sqlRepoContext, sqlRepoContext.newQuery(), objectLoader);
    }

    /** Factory method useful for cases when specific sqlQuery instance needs to be provided. */
    public static <S, Q extends FlexibleRelationalPathBase<R>, R> SqaleQueryContext<S, Q, R> from(
            Class<S> schemaType,
            SqaleRepoContext sqlRepoContext,
            SQLQuery<?> sqlQuery,
            SqaleObjectLoader objectLoader) {
        return from(sqlRepoContext.getMappingBySchemaType(schemaType),
                sqlRepoContext, sqlQuery, objectLoader);
    }

    public static <S, Q extends FlexibleRelationalPathBase<R>, R> SqaleQueryContext<S, Q, R> from(
            SqaleTableMapping<S, Q, R> rootMapping,
            SqaleRepoContext sqlRepoContext,
            SQLQuery<?> sqlQuery,
            SqaleObjectLoader objectLoader) {
        Q rootPath = rootMapping.defaultAlias();
        SQLQuery<?> query = sqlQuery.from(rootPath);
        // Turns on validations of aliases, does not ignore duplicate JOIN expressions,
        // we must take care of unique alias names for JOINs, which is what we want.
        query.getMetadata().setValidate(true);

        return new SqaleQueryContext<>(
                rootPath, rootMapping, sqlRepoContext, query, objectLoader);
    }

    /** Constructor for the root context. */
    private SqaleQueryContext(
            Q entityPath,
            SqaleTableMapping<S, Q, R> mapping,
            SqaleRepoContext sqlRepoContext,
            SQLQuery<?> query,
            SqaleObjectLoader objectLoader) {
        super(entityPath, mapping, sqlRepoContext, query);
        this.objectLoader = objectLoader;
        this.queryMapping = mapping;
        // We want to register the initial FROM alias just in case the same mapping is needed again.
        // This can happen with ref target dereferencing or with referenced-by filters.
        uniqueAliasName(mapping.defaultAliasName());
    }

    private SqaleQueryContext(
            Q entityPath,
            SqaleTableMapping<S, Q, R> mapping,
            SqaleQueryContext<?, ?, ?> parentContext,
            SQLQuery<?> sqlQuery) {
        super(entityPath, mapping, parentContext, sqlQuery);
        this.objectLoader = parentContext.objectLoader;
        this.queryMapping = mapping;
    }

    private SqaleQueryContext(Q entityPath, QueryTableMapping<S, Q, R> queryTableMapping,
            SqaleQueryContext<S, Q, R> parentContext, SQLQuery<?> sqlQuery,
            SqaleNestedMapping<S, Q, R> nestedMapping) {
        super(entityPath, queryTableMapping, parentContext, sqlQuery);
        this.objectLoader = parentContext.objectLoader;
        this.queryMapping = nestedMapping;
    }

    @Override
    public Predicate process(@NotNull ObjectFilter filter) throws RepositoryException {
        // To compare with old repo see: QueryInterpreter.findAndCreateRestrictionInternal
        if (filter instanceof InOidFilter) {
            return new InOidFilterProcessor(this).process((InOidFilter) filter);
        } else if (filter instanceof OrgFilter) {
            return new OrgFilterProcessor(this).process((OrgFilter) filter);
        } else if (filter instanceof FullTextFilter) {
            return new FullTextFilterProcessor(this).process((FullTextFilter) filter);
        } else if (filter instanceof ExistsFilter) {
            return new ExistsFilterProcessor<>(this).process((ExistsFilter) filter);
        } else if (filter instanceof OwnedByFilter) {
            return new OwnedByFilterProcessor<>(this).process((OwnedByFilter) filter);
        } else if (filter instanceof ReferencedByFilter) {
            return new ReferencedByFilterProcessor<>(this).process((ReferencedByFilter) filter);
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
    public <TS, TQ extends FlexibleRelationalPathBase<TR>, TR> SqlQueryContext<TS, TQ, TR>
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

    public <T extends ObjectType> PrismObject<T> loadObject(
            JdbcSession jdbcSession, Class<T> objectType, UUID oid,
            Collection<SelectorOptions<GetOperationOptions>> options) {
        try {
            //noinspection unchecked
            return (PrismObject<T>) objectLoader.readByOid(jdbcSession, objectType, oid, options)
                    .asPrismObject();
        } catch (SchemaException | ObjectNotFoundException e) {
            throw new SystemException(e);
        }
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

    @Override
    public QueryModelMapping<S, Q, R> queryMapping() {
        return queryMapping;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <TS, TQ extends FlexibleRelationalPathBase<TR>, TR> SqlQueryContext<TS, TQ, TR> nestedContext(
            SqaleNestedMapping<TS, TQ, TR> nestedMapping) {
        return new SqaleQueryContext(entityPath, mapping(), this, sqlQuery, nestedMapping);
    }

    @Override
    public Predicate processFuzzyFilter(
            FuzzyStringMatchFilter<?> filter, Expression<?> path, ValueFilterValues<?, ?> values)
            throws QueryException {
        FuzzyMatchingMethod method = filter.getMatchingMethod();
        if (method instanceof Levenshtein levenshtein) {
            var func = Expressions.numberTemplate(Integer.class,
                    "levenshtein_less_equal({0}, {1}, {2})",
                    path, String.valueOf(values.singleValue()), levenshtein.getThresholdRequired());
            // Lower value means more similar
            return levenshtein.isInclusive() ?
                    func.loe(levenshtein.getThresholdRequired()) :
                    func.lt(levenshtein.getThresholdRequired());
        } else if (method instanceof Similarity spec) {
            var func = Expressions.numberTemplate(Float.class,
                    "similarity({0}, {1})",
                    path, String.valueOf(values.singleValue()));
            // Higher value means more similar
            return spec.isInclusive() ?
                    func.goe(spec.getThresholdRequired()) :
                    func.gt(spec.getThresholdRequired());
        }

        return super.processFuzzyFilter(filter, path, values);
    }

}
