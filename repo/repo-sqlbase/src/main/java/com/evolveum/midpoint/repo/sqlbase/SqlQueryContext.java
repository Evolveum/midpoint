/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import java.sql.Connection;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.ObjectFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlDetailFetchMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Context information about SQL query.
 * Works as a kind of accumulator where information are added as the object query is interpreted.
 * It is also used as an entry point for {@link FilterProcessor} processing for this query.
 * And finally, it also executes the query, because this way it is more practical to contain
 * all the needed parametrized types without using Class-type parameters.
 * <p>
 * This object <b>does not handle SQL connections or transaction</b> in any way, any connection
 * needed is provided from the outside.
 *
 * @param <S> schema type, used by encapsulated mapping
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public abstract class SqlQueryContext<S, Q extends FlexibleRelationalPathBase<R>, R>
        implements FilterProcessor<ObjectFilter> {

    /**
     * Default page size if pagination is requested, that is offset is set, but maxSize is not.
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * If no other limit is used for query this limit will be used for sanity reasons.
     */
    public static final long NO_PAGINATION_LIMIT = 10_000;

    /**
     * Number of values (identifiers) used in the IN clause to-many fetching selects.
     * This works effectively as factor of how bad N+1 select is, it's at most N/this-limit+1 bad.
     * For obvious reasons, this works only for non-composite PKs (IDs) on the master entity.
     */
    public static final int MAX_ID_IN_FOR_TO_MANY_FETCH = 100;

    protected final SQLQuery<?> sqlQuery;

    protected final Q entityPath;
    protected final QueryTableMapping<S, Q, R> entityPathMapping;
    protected final SqlRepoContext sqlRepoContext;
    protected final SqlTransformerSupport transformerSupport;

    protected boolean notFilterUsed = false;

    // options stored to modify select clause and also to affect transformation
    protected Collection<SelectorOptions<GetOperationOptions>> options;

    protected SqlQueryContext(
            Q entityPath,
            QueryTableMapping<S, Q, R> mapping,
            SqlRepoContext sqlRepoContext,
            SqlTransformerSupport transformerSupport,
            SQLQuery<?> query) {
        this.entityPath = entityPath;
        this.entityPathMapping = mapping;
        this.sqlRepoContext = sqlRepoContext;
        this.transformerSupport = transformerSupport;
        this.sqlQuery = query;
    }

    public Q root() {
        return path();
    }

    public <T extends FlexibleRelationalPathBase<?>> T root(Class<T> rootType) {
        return path(rootType);
    }

    @Override
    public Predicate process(ObjectFilter filter) throws QueryException {
        if (filter == null) {
            return null;
        }

        Predicate condition = new ObjectFilterProcessor(this).process(filter);
        sqlQuery.where(condition);
        // probably not used after added to where, but let's respect the contract
        return condition;
    }

    /**
     * This takes care of {@link ObjectPaging} which includes ordering.
     */
    public void processObjectPaging(ObjectPaging paging) throws QueryException {
        if (paging == null) {
            return;
        }

        processOrdering(paging.getOrderingInstructions());

        Integer offset = paging.getOffset();
        // we take null offset as no paging at all
        if (offset != null) {
            sqlQuery.offset(offset.longValue());
            Integer pageSize = paging.getMaxSize();
            sqlQuery.limit(pageSize != null ? pageSize.longValue() : DEFAULT_PAGE_SIZE);
        }
    }

    private void processOrdering(List<? extends ObjectOrdering> orderings) throws QueryException {
        for (ObjectOrdering ordering : orderings) {
            ItemPath orderByItemPath = ordering.getOrderBy();
            if (!(orderByItemPath.isSingleName())) {
                throw new QueryException(
                        "ORDER BY is not possible for complex paths: " + orderByItemPath);
            }
            Path<?> path = mapping().primarySqlPath(orderByItemPath.asSingleNameOrFail(), this);
            if (!(path instanceof ComparableExpressionBase)) {
                throw new QueryException(
                        "ORDER BY is not possible for non-comparable path: " + orderByItemPath);
            }

            if (ordering.getDirection() == OrderDirection.DESCENDING) {
                sqlQuery.orderBy(((ComparableExpressionBase<?>) path).desc());
            } else {
                sqlQuery.orderBy(((ComparableExpressionBase<?>) path).asc());
            }
        }
    }

    /**
     * Returns page of results with each row represented by a Tuple containing {@link R} and then
     * individual paths for extension columns, see {@code extensionColumns} in {@link QueryTableMapping}.
     */
    public PageOf<Tuple> executeQuery(Connection conn) throws QueryException {
        SQLQuery<?> query = this.sqlQuery.clone(conn);
        if (query.getMetadata().getModifiers().getLimit() == null) {
            query.limit(NO_PAGINATION_LIMIT);
        }

        // SQL logging is on DEBUG level of: com.querydsl.sql
        Q entity = root();
        List<Tuple> data = query
                .select(buildSelectExpressions(entity, query))
                .fetch();

        // TODO: run fetchers selectively based on options?
        if (!mapping().detailFetchMappers().isEmpty()) {
            // we don't want to extract R if no mappers exist, otherwise we want to do it only once
            List<R> dataEntities = data.stream()
                    .map(t -> t.get(entity))
                    .collect(Collectors.toList());
            for (SqlDetailFetchMapper<R, ?, ?, ?> fetcher : mapping().detailFetchMappers()) {
                fetcher.execute(sqlRepoContext, () -> sqlRepoContext.newQuery(conn), dataEntities);
            }
        }

        return new PageOf<>(data, PageOf.PAGE_NO_PAGINATION, 0);
    }

    private @NotNull Expression<?>[] buildSelectExpressions(Q entity, SQLQuery<?> query) {
        Path<?>[] defaultExpressions = mapping().selectExpressions(entity, options);
        if (!query.getMetadata().isDistinct() || query.getMetadata().getOrderBy().isEmpty()) {
            return defaultExpressions;
        }

        // If DISTINCT is used with ORDER BY then anything in ORDER BY must be in SELECT too
        List<Expression<?>> expressions = new ArrayList<>(Arrays.asList(defaultExpressions));
        for (OrderSpecifier<?> orderSpecifier : query.getMetadata().getOrderBy()) {
            Expression<?> orderPath = orderSpecifier.getTarget();
            if (!expressions.contains(orderPath)) {
                expressions.add(orderPath);
            }
        }
        return expressions.toArray(new Expression<?>[0]);
    }

    public int executeCount(Connection conn) {
        return (int) sqlQuery.clone(conn)
                // select not needed here, it would only initialize projection unnecessarily
                .fetchCount();
    }

    /**
     * Adds new LEFT JOIN to the query and returns {@link SqlQueryContext} for this join path.
     *
     * @param <DQ> query type for the JOINed table
     * @param <DR> row type related to the {@link DQ}
     * @param joinType entity path type for the JOIN
     * @param joinOnPredicateFunction bi-function producing ON predicate for the JOIN
     */
    public <DQ extends FlexibleRelationalPathBase<DR>, DR> SqlQueryContext<?, DQ, DR> leftJoin(
            @NotNull Class<DQ> joinType,
            @NotNull BiFunction<Q, DQ, Predicate> joinOnPredicateFunction) {
        return leftJoin(sqlRepoContext.getMappingByQueryType(joinType), joinOnPredicateFunction);
    }

    /**
     * Adds new LEFT JOIN to the query and returns {@link SqlQueryContext} for this join path.
     *
     * @param <DQ> query type for the JOINed table
     * @param <DR> row type related to the {@link DQ}
     * @param targetMapping mapping for the JOIN target query type
     * @param joinOnPredicateFunction bi-function producing ON predicate for the JOIN
     */
    public <DQ extends FlexibleRelationalPathBase<DR>, DR> SqlQueryContext<?, DQ, DR> leftJoin(
            @NotNull QueryTableMapping<?, DQ, DR> targetMapping,
            @NotNull BiFunction<Q, DQ, Predicate> joinOnPredicateFunction) {
        String aliasName = uniqueAliasName(targetMapping.defaultAliasName());
        DQ joinPath = targetMapping.newAlias(aliasName);
        sqlQuery.leftJoin(joinPath).on(joinOnPredicateFunction.apply(path(), joinPath));
        SqlQueryContext<?, DQ, DR> newQueryContext = deriveNew(joinPath, targetMapping);

        // for JOINed context we want to preserve "NOT" status (unlike for subqueries)
        if (notFilterUsed) {
            newQueryContext.markNotFilterUsage();
        }

        return newQueryContext;
    }

    protected abstract <DQ extends FlexibleRelationalPathBase<DR>, DR> SqlQueryContext<?, DQ, DR>
    deriveNew(DQ newPath, QueryTableMapping<?, DQ, DR> newMapping);

    public String uniqueAliasName(String baseAliasName) {
        Set<String> joinAliasNames =
                sqlQuery.getMetadata().getJoins().stream()
                        .map(j -> j.getTarget().toString())
                        .collect(Collectors.toSet());

        // number the alias if not unique (starting with 2, implicit 1 is without number)
        String aliasName = baseAliasName;
        int sequence = 1;
        while (joinAliasNames.contains(aliasName)) {
            sequence += 1;
            aliasName = baseAliasName + sequence;
        }
        return aliasName;
    }

    public void processOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        this.options = options;
        if (options == null || options.isEmpty()) {
            return;
        }

        // TODO what other options we need here? can they all be processed after filter?

        // Dropping DISTINCT without JOIN is OK for object/container queries where select
        // already contains distinct columns (OID or owner_oid+cid).
        if (GetOperationOptions.isDistinct(SelectorOptions.findRootOptions(options))
                && sqlQuery.getMetadata().getJoins().size() > 1) {
            sqlQuery.distinct();
        }
    }

    /**
     * Transforms result page with (bean + extension columns) tuple to schema type.
     */
    public PageOf<S> transformToSchemaType(PageOf<Tuple> result)
            throws SchemaException, QueryException {
        try {
            SqlTransformer<S, Q, R> transformer = createTransformer();
            return result.map(row -> transformer.toSchemaObjectSafe(row, root(), options));
        } catch (SqlTransformer.SqlTransformationException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SchemaException) {
                throw (SchemaException) cause;
            } else if (cause instanceof QueryException) {
                throw (QueryException) cause;
            } else {
                throw e;
            }
        }
    }

    /**
     * Creates transformer for the {@link #entityPathMapping}.
     * Made abstract, because the way how to create the transformer can differ on the type
     * of context that is needed to do it (typically providing various components).
     */
    protected abstract SqlTransformer<S, Q, R> createTransformer();

    /**
     * Returns wrapped query if usage of Querydsl API is more convenient.
     */
    public SQLQuery<?> sqlQuery() {
        return sqlQuery;
    }

    /**
     * Returns entity path of this context.
     */
    public Q path() {
        return entityPath;
    }

    public <T extends FlexibleRelationalPathBase<?>> T path(Class<T> pathType) {
        return pathType.cast(entityPath);
    }

    public QueryTableMapping<S, Q, R> mapping() {
        return entityPathMapping;
    }

    public void markNotFilterUsage() {
        notFilterUsed = true;
    }

    public boolean isNotFilterUsed() {
        return notFilterUsed;
    }

    public SqlRepoContext sqlRepoContext() {
        return sqlRepoContext;
    }

    public PrismContext prismContext() {
        return transformerSupport.prismContext();
    }

    public <T> Class<? extends T> qNameToSchemaClass(@NotNull QName qName) {
        return transformerSupport.qNameToSchemaClass(qName);
    }

    public CanonicalItemPath createCanonicalItemPath(@NotNull ItemPath itemPath) {
        return transformerSupport.prismContext().createCanonicalItemPath(itemPath);
    }

    @NotNull
    public QName normalizeRelation(QName qName) {
        return transformerSupport.normalizeRelation(qName);
    }
}
