/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.querydsl.core.QueryMetadata;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.sql.SQLQuery;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sqlbase.filtering.*;
import com.evolveum.midpoint.repo.sqlbase.mapping.*;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;

/**
 * Execution context of the SQL query.
 * Works as a kind of accumulator where information are added as the object query is interpreted.
 * The object has a couple of overlapping responsibilities:
 *
 * * It implements {@link FilterProcessor} and is used as an entry point for filter processing for the query.
 * * It executes the query, returning {@link PageOf} low-level rows, see {@link #executeQuery}.
 * * It transforms the row beans to midPoint objects using {@link #transformToSchemaType}.
 *
 * {@link QueryTableMapping} is crucial for all these steps and flow of the execution goes a lot between
 * the {@link SqlQueryExecutor}, this class and particular methods of the mapping.
 * Anything specific for a particular type/table should be part of the mapping logic.
 *
 * The mapping contract should cover all the needs of this execution context.
 * It can be extended if needed, but always think whether existing mechanisms are not enough already.
 * E.g. if you need to post-process the low level result, there is a way how to do it and it allows for things
 * like loading all the detail table rows in a single query.
 * See {@link #transformToSchemaType} for notes how this allows for inter-row state keeping as well.
 *
 * [NOTE]
 * Implementation note:
 * There was an option to keep this as an information accumulator only and do the execution
 * elsewhere, but it proved more practical to utilize all the contained parameterized types.
 * Methods executing the query and processing the result would need to be parameterized the same way
 * this context already is - so it was better to use the types here.
 *
 * [NOTE]
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
    private final SqlRepoContext sqlRepoContext;

    private final SqlQueryContext<?, ?, ?> parent;

    /**
     * This is just poor man way to track at least parent item path contexts (one-to-one for "../*" filters/ordering)
     */
    private SqlQueryContext<?, ?, ?> parentItemContext;

    protected boolean notFilterUsed = false;

    // options stored to modify select clause and also to affect mapping
    protected Collection<SelectorOptions<GetOperationOptions>> options;

    private final Set<String> usedAliases;

    /** Constructor for root query context. */
    protected SqlQueryContext(
            Q entityPath,
            QueryTableMapping<S, Q, R> mapping,
            SqlRepoContext sqlRepoContext,
            SQLQuery<?> query) {
        this.entityPath = entityPath;
        this.entityPathMapping = mapping;
        this.sqlRepoContext = sqlRepoContext;
        this.sqlQuery = query;
        this.parent = null;
        this.usedAliases = new HashSet<>();
    }

    public SqlQueryContext<?, ?, ?> getParentItemContext() {
        return parentItemContext;
    }

    public void setParentItemContext(SqlQueryContext<?, ?, ?> parentItemContext) {
        this.parentItemContext = parentItemContext;
    }

    /** Constructor for derived context or sub-context, e.g. JOIN, EXISTS, etc. */
    protected SqlQueryContext(
            Q entityPath,
            QueryTableMapping<S, Q, R> mapping,
            SqlQueryContext<?, ?, ?> parentContext,
            SQLQuery<?> sqlQuery) {
        this.entityPath = entityPath;
        this.entityPathMapping = mapping;
        this.sqlRepoContext = parentContext.repositoryContext();
        this.sqlQuery = sqlQuery;
        this.parent = parentContext;
        this.usedAliases = null;
    }

    public Q root() {
        return path();
    }

    public <T extends FlexibleRelationalPathBase<?>> T root(Class<T> rootType) {
        return path(rootType);
    }

    /**
     * Processes the object filter and sets the WHERE clause.
     *
     * This is different from {@link #process(ObjectFilter)} that just creates a predicate.
     * That method is used in this one and {@link SQLQuery#where(Predicate)} is called.
     */
    public void processFilter(ObjectFilter filter) throws RepositoryException {
        if (filter != null) {
            Predicate predicate = process(filter);
            try {
                sqlQuery.where(predicate);
            } catch (IllegalArgumentException e) {
                throw new RepositoryException("Query construction problem, current query: "
                        + sqlQuery + "\n  Predicate: " + predicate, e);
            }
        }
    }

    /**
     * Implements contract for {@link FilterProcessor} working as a top-level dispatcher
     * to concrete filter types.
     * This is a universal/generic filter processor that dispatches to the actual filter processor
     * based on the filter type.
     * It is used both as an entry point for the root filter of the query, but also when various
     * structural filters need to resolve their components (e.g. AND uses this for its components).
     *
     * *This only returns the created predicate, compare with {@link #processFilter}.*
     *
     * Some subtypes of {@link ObjectFilter} from Prism API are not supported here, see subclasses.
     */
    @Override
    public Predicate process(@NotNull ObjectFilter filter) throws RepositoryException {
        // To compare with old repo see: QueryInterpreter.findAndCreateRestrictionInternal
        if (filter instanceof NaryLogicalFilter) {
            return new NaryLogicalFilterProcessor(this)
                    .process((NaryLogicalFilter) filter);
        } else if (filter instanceof NotFilter) {
            return new NotFilterProcessor(this)
                    .process((NotFilter) filter);
        } else if (filter instanceof ValueFilter) {
            // here are the values applied (ref/property value filters)
            return new ValueFilterProcessor<>(this)
                    .process((ValueFilter<?, ?>) filter);
        } else if (filter instanceof AllFilter) {
            return QuerydslUtils.EXPRESSION_TRUE;
        } else if (filter instanceof NoneFilter) {
            return QuerydslUtils.EXPRESSION_FALSE;
        } else {
            throw new QueryException("Unsupported filter " + filter);
        }
    }

    /**
     * This method takes care of {@link ObjectPaging} which includes ordering.
     */
    public void processObjectPaging(ObjectPaging paging) throws RepositoryException {
        if (paging == null) {
            return;
        }

        processOrdering(paging.getOrderingInstructions());

        Integer offset = paging.getOffset();
        Integer maxSize = paging.getMaxSize();
        // we take null offset as no paging at all
        if (offset != null) {
            sqlQuery.offset(offset.longValue());
            sqlQuery.limit(maxSize != null ? maxSize.longValue() : DEFAULT_PAGE_SIZE);
        } else if (maxSize != null) {
            // we respect limit even without offset, other ways can be used (e.g. WHERE OID > ...)
            sqlQuery.limit(maxSize);
        }
    }

    private void processOrdering(List<? extends ObjectOrdering> orderings)
            throws RepositoryException {
        for (ObjectOrdering ordering : orderings) {
            ItemPath orderByItemPath = ordering.getOrderBy();
            Expression<?> expression = orderingPath(orderByItemPath);
            if (expression instanceof ComparableExpressionBase) {
                if (ordering.getDirection() == OrderDirection.DESCENDING) {
                    sqlQuery.orderBy(((ComparableExpressionBase<?>) expression).desc());
                } else {
                    sqlQuery.orderBy(((ComparableExpressionBase<?>) expression).asc());
                }
            } else if (expression instanceof SqlOrderableExpression) {
                // For corner cases you can declare ordering by the whole table, which is treated differently.
                ((SqlOrderableExpression) expression).orderBy(sqlQuery, ordering);
            } else {
                throw new QueryException(
                        "ORDER BY is not possible for non-comparable path: " + orderByItemPath);
            }
        }
    }

    public <CQ extends FlexibleRelationalPathBase<CR>, CR> ResolveResult<CQ, CR> resolvePathWithJoins(ItemPath inputPath) throws RepositoryException {
        ItemPath path = inputPath;
        QueryModelMapping<?, CQ, CR> mapping = (QueryModelMapping<?, CQ, CR>) entityPathMapping;
        SqlQueryContext<?, CQ, CR> context = (SqlQueryContext<?, CQ, CR>) this;

        // We need definition for proper extension support.
        // For other cases it's safe for this to become null.
        PrismContainerDefinition<?> containerDefinition =
                (PrismContainerDefinition<?>) entityPathMapping.itemDefinition();

        while (path.size() > 1) {
            ItemRelationResolver<CQ, CR, ?, ?> resolver = mapping.relationResolver(path); // Resolves only first element
            ItemRelationResolver.ResolutionResult<?, ?> resolution = resolver.resolveUsingJoin(context);
            if (resolution.subquery) {
                throw new QueryException("Item path '" + inputPath
                        + "' cannot be used for ordering because subquery is used to resolve it.");
            }
            // CQ/CR for the next loop may be actually different from before, but that's OK
            mapping = (QueryModelMapping<?, CQ, CR>) resolution.mapping;
            context = (SqlQueryContext<?, CQ, CR>) resolution.context;

            if (containerDefinition != null) {
                if (ItemPath.isParent(path.first())) {
                    // Should work if parent is container (almost always)
                    containerDefinition = (PrismContainerDefinition<?>) mapping.itemDefinition();
                } else {
                    containerDefinition = containerDefinition.findLocalItemDefinition(
                            path.firstToName(), PrismContainerDefinition.class, false);
                }
            }

            path = path.rest();
        }

        QName first = path.firstToQName();
        ItemDefinition<?> definition = first instanceof ItemName && containerDefinition != null
                ? containerDefinition.findItemDefinition((ItemName) first)
                : null;

        ItemSqlMapper<CQ, CR> mapper = mapping.itemMapper(first);
        return new ResolveResult<CQ, CR>(mapper, context, definition);
    }

    public static class ResolveResult<CQ extends FlexibleRelationalPathBase<CR>, CR> {
        public final ItemSqlMapper<CQ, CR> mapper;
        public final SqlQueryContext<?, CQ, CR> context;
        public final ItemDefinition<?> definition;

        public ResolveResult(ItemSqlMapper<CQ, CR> mapper, SqlQueryContext<?, CQ, CR> context, ItemDefinition<?> definition) {
            this.mapper = mapper;
            this.context = context;
            this.definition = definition;
        }
    }

    /**
     * @param <CQ> current entity query path type, can change during multi-segment path resolution
     * @param <CR> row type related to {@link CQ}
     */
    @SuppressWarnings("unchecked")
    private <CQ extends FlexibleRelationalPathBase<CR>, CR> Expression<?> orderingPath(
            ItemPath orderByItemPath) throws RepositoryException {

        ItemPath path = orderByItemPath;
        QueryModelMapping<?, CQ, CR> mapping = (QueryModelMapping<?, CQ, CR>) entityPathMapping;
        SqlQueryContext<?, CQ, CR> context = (SqlQueryContext<?, CQ, CR>) this;

        // We need definition for proper extension support.
        // For other cases it's safe for this to become null.
        PrismContainerDefinition<?> containerDefinition =
                (PrismContainerDefinition<?>) entityPathMapping.itemDefinition();

        while (path.size() > 1) {
            boolean skipJoinIfPossible = !path.isEmpty() && PrismConstants.T_PARENT.equals(path.first());

            if (skipJoinIfPossible) {
                if (context.getParentItemContext() != null) {
                    context = (SqlQueryContext<?, CQ, CR>) context.getParentItemContext();
                    mapping = context.queryMapping();
                } else {
                    Pair<QueryModelMapping<?, CQ, CR>, SqlQueryContext<?, CQ, CR>> pair =
                            resolveContextAndMapping(orderByItemPath, path, mapping, context);

                    context.setParentItemContext(pair.getRight());

                    mapping = pair.getLeft();
                    context = pair.getRight();
                }
            } else {
                Pair<QueryModelMapping<?, CQ, CR>, SqlQueryContext<?, CQ, CR>> pair =
                        resolveContextAndMapping(orderByItemPath, path, mapping, context);

                mapping = pair.getLeft();
                context = pair.getRight();
            }

            if (containerDefinition != null) {
                if (ItemPath.isParent(path.first())) {
                    // Should work if parent is container (almost always)
                    containerDefinition = (PrismContainerDefinition<?>) mapping.itemDefinition();
                } else {
                    containerDefinition = containerDefinition.findLocalItemDefinition(
                            path.firstToName(), PrismContainerDefinition.class, false);
                }
            }

            path = path.rest();
        }

        QName first = path.firstToQName();
        ItemDefinition<?> definition = first instanceof ItemName && containerDefinition != null
                ? containerDefinition.findItemDefinition((ItemName) first)
                : null;

        ItemSqlMapper<CQ, CR> mapper = mapping.itemMapper(first);
        return mapper.primaryPath(context.path(), definition);
    }

    private <CQ extends FlexibleRelationalPathBase<CR>, CR> Pair<QueryModelMapping<?, CQ, CR>, SqlQueryContext<?, CQ, CR>>
    resolveContextAndMapping(
            ItemPath orderByItemPath, ItemPath path, QueryModelMapping<?, CQ, CR> mapping, SqlQueryContext<?, CQ, CR> context)
            throws QueryException {

        ItemRelationResolver<CQ, CR, ?, ?> resolver = mapping.relationResolver(path); // Resolves only first element
        ItemRelationResolver.ResolutionResult<?, ?> resolution = resolver.resolve(context);
        if (resolution.subquery) {
            throw new QueryException("Item path '" + orderByItemPath
                    + "' cannot be used for ordering because subquery is used to resolve it.");
        }
        // CQ/CR for the next loop may be actually different from before, but that's OK
        mapping = (QueryModelMapping<?, CQ, CR>) resolution.mapping;
        context = (SqlQueryContext<?, CQ, CR>) resolution.context;

        return Pair.of(mapping, context);
    }

    /**
     * Returns page of results with each row represented by a {@link Tuple}.
     * Tuple contains expressions specified by {@link QueryTableMapping#selectExpressions},
     * see {@link #buildSelectExpressions} for details.
     * This may for example be {@link R} (representing the whole entity) and then individual paths
     * for extension columns, see {@code extensionColumns} in {@link QueryTableMapping}.
     *
     * {@link QueryTableMapping} has many responsibilities in the process:
     *
     * * {@link #options} are used to amend the select expression list; the options later enter
     * as a parameter to most methods related to the transformation from row to midPoint object.
     * * {@link QueryTableMapping#createRowTransformer} (used later by {@link #transformToSchemaType})
     * allows for low-level result list processing, e.g. fetching any additional objects efficiently.
     */
    public PageOf<Tuple> executeQuery(JdbcSession jdbcSession) throws QueryException {
        SQLQuery<?> query = sqlQuery.clone(jdbcSession.connection());
        if (query.getMetadata().getModifiers().getLimit() == null) {
            query.limit(NO_PAGINATION_LIMIT);
            // TODO indicate incomplete result?
        }

        // see com.evolveum.midpoint.repo.sqlbase.querydsl.SqlLogger for logging details
        Q entity = root();
        List<Tuple> data = query
                .select(buildSelectExpressions(entity, query))
                .fetch();

        // TODO: This is currently used for old audit only.
        //  New audit would work too as the ID there is unique, but we want to use timestamp column
        //  which is a partition key. Fetchers are not suitable to do that.
        // Fetchers are now superseded by ResultListRowTransformer#beforeTransformation()
        Collection<SqlDetailFetchMapper<R, ?, ?, ?>> detailFetchMappers =
                entityPathMapping.detailFetchMappers();
        if (!detailFetchMappers.isEmpty()) {
            // we don't want to extract R if no mappers exist, otherwise we want to do it only once
            List<R> dataEntities = data.stream()
                    .map(t -> t.get(entity))
                    .collect(Collectors.toList());
            for (SqlDetailFetchMapper<R, ?, ?, ?> fetcher : detailFetchMappers) {
                fetcher.execute(sqlRepoContext, jdbcSession::newQuery, dataEntities);
            }
        }

        return new PageOf<>(data, PageOf.PAGE_NO_PAGINATION, 0);
    }

    private @NotNull Expression<?>[] buildSelectExpressions(Q entity, SQLQuery<?> query) {
        Path<?>[] defaultExpressions = entityPathMapping.selectExpressions(entity, options);
        QueryMetadata metadata = query.getMetadata();
        if (!metadata.isDistinct() || metadata.getOrderBy().isEmpty()) {
            return defaultExpressions;
        }

        // If DISTINCT is used with ORDER BY then anything in ORDER BY must be in SELECT too
        List<Expression<?>> expressions = new ArrayList<>(Arrays.asList(defaultExpressions));
        for (OrderSpecifier<?> orderSpecifier : metadata.getOrderBy()) {
            Expression<?> orderPath = orderSpecifier.getTarget();
            if (!expressions.contains(orderPath)) {
                expressions.add(orderPath);
            }
        }
        return expressions.toArray(new Expression<?>[0]);
    }

    public int executeCount(JdbcSession jdbcSession) {
        return (int) sqlQuery.clone(jdbcSession.connection())
                // select not needed here, it would only initialize projection unnecessarily
                .fetchCount();
    }

    /**
     * Adds new LEFT JOIN to the query and returns {@link SqlQueryContext} for this join path.
     * The returned context still uses the same SQL query; any further filter processing will
     * add WHERE conditions to the original query, but the conditions use the new alias.
     *
     * @param <TQ> query type for the JOINed (target) table
     * @param <TR> row type related to the {@link TQ}
     * @param targetMapping mapping for the JOIN target query type
     * @param joinOnPredicateFunction bi-function producing ON predicate for the JOIN
     */
    public <TS, TQ extends FlexibleRelationalPathBase<TR>, TR> SqlQueryContext<TS, TQ, TR> leftJoin(
            @NotNull QueryTableMapping<TS, TQ, TR> targetMapping,
            @NotNull BiFunction<Q, TQ, Predicate> joinOnPredicateFunction) {
        String aliasName = uniqueAliasName(targetMapping.defaultAliasName());
        TQ joinPath = targetMapping.newAlias(aliasName);
        sqlQuery.leftJoin(joinPath).on(joinOnPredicateFunction.apply(path(), joinPath));
        SqlQueryContext<TS, TQ, TR> newQueryContext = newSubcontext(joinPath, targetMapping);

        // for JOINed context we want to preserve "NOT" status (unlike for subqueries)
        if (notFilterUsed) {
            newQueryContext.markNotFilterUsage();
        }

        // FIXME: Should we track joins? probably yes

        return newQueryContext;
    }

    /**
     * Creates new subquery, see {@link #subquery(QueryTableMapping)} for more.
     *
     * @param subqueryType entity path type the subquery
     */
    public <TS, TQ extends FlexibleRelationalPathBase<TR>, TR> SqlQueryContext<TS, TQ, TR> subquery(
            @NotNull Class<TQ> subqueryType) {
        return subquery(sqlRepoContext.getMappingByQueryType(subqueryType));
    }

    /**
     * Creates new subquery and returns {@link SqlQueryContext} for it, typically for (NOT) EXISTS.
     * Call to {@link SQLQuery#exists()} can't be here, because it's a predicate creating call
     * that we may need to execute when returning the predicate inside the filter processor.
     * See `TypeFilterProcessor` from `repo-sqale` for example.
     *
     * @param <TQ> query type for the subquery table
     * @param <TR> row type related to the {@link TQ}
     * @param targetMapping mapping for the subquery type
     */
    public <TS, TQ extends FlexibleRelationalPathBase<TR>, TR> SqlQueryContext<TS, TQ, TR> subquery(
            @NotNull QueryTableMapping<TS, TQ, TR> targetMapping) {
        // We don't want to collide with other JOIN aliases, but no need to check other subqueries.
        String aliasName = uniqueAliasName(targetMapping.defaultAliasName());
        TQ subqueryPath = targetMapping.newAlias(aliasName);
        SQLQuery<?> subquery = new SQLQuery<Integer>()
                .select(QuerydslUtils.EXPRESSION_ONE)
                .from(subqueryPath);
        return newSubcontext(subqueryPath, targetMapping, subquery);
    }

    /**
     * Contract to implement to obtain derived (e.g. joined) query context.
     *
     * @param <TQ> query type for the new (target) table
     * @param <TR> row type related to the {@link TQ}
     */
    public abstract <TS, TQ extends FlexibleRelationalPathBase<TR>, TR>
    SqlQueryContext<TS, TQ, TR> newSubcontext(TQ newPath, QueryTableMapping<TS, TQ, TR> newMapping);

    /**
     * Contract to implement to obtain derived (e.g. subquery) query context.
     *
     * @param <TQ> query type for the new (target) table
     * @param <TR> row type related to the {@link TQ}
     */
    protected abstract <TS, TQ extends FlexibleRelationalPathBase<TR>, TR>
    SqlQueryContext<TS, TQ, TR> newSubcontext(
            TQ newPath, QueryTableMapping<TS, TQ, TR> newMapping, SQLQuery<?> query);

    /** Adjusts the alias name to make it unique and registers the name as used. */
    public String uniqueAliasName(String baseAliasName) {
        if (parent != null) {
            // We always want to use root context to make the names unique for the whole query.
            return parent.uniqueAliasName(baseAliasName);
        }

        // Number the alias if not unique (starting with 2, implicit 1 is without number).
        String aliasName = baseAliasName;
        int sequence = 1;
        while (usedAliases.contains(aliasName)) {
            sequence += 1;
            aliasName = baseAliasName + sequence;
        }
        usedAliases.add(aliasName);
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
     * JDBC session is provided as it may be needed for additional fetches.
     * Instead of calling some transformation method row-by-row, transformer object is provided
     * by the table mapper - which allows for potentially stateful processing.
     */
    public PageOf<S> transformToSchemaType(PageOf<Tuple> result, JdbcSession jdbcSession)
            throws SchemaException, QueryException {
        try {
            ResultListRowTransformer<S, Q, R> rowTransformer =
                    entityPathMapping.createRowTransformer(this, jdbcSession, options);

            rowTransformer.beforeTransformation(result.content(), entityPath);
            PageOf<S> transformedResult = result.map(row -> rowTransformer.transform(row, entityPath));
            rowTransformer.finishTransformation();

            return transformedResult;
        } catch (RepositoryMappingException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SchemaException) {
                throw (SchemaException) cause;
            } else if (cause instanceof QueryException) {
                throw (QueryException) cause;
            } else {
                throw e;
            }
        } catch (TunnelException e) {
            if (e.getCause() instanceof SchemaException schemaEx) {
                throw schemaEx;
            }
            throw e;
        }
    }

    /**
     * Returns wrapped query if usage of Querydsl API is more convenient.
     */
    public SQLQuery<?> sqlQuery() {
        return sqlQuery;
    }

    public SqlQueryContext<?, ?, ?> parentContext() {
        return parent;
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

    public QueryModelMapping<S, Q, R> queryMapping() {
        return mapping();
    }

    public void markNotFilterUsage() {
        notFilterUsed = true;
    }

    public boolean isNotFilterUsed() {
        return notFilterUsed;
    }

    public SqlRepoContext repositoryContext() {
        return sqlRepoContext;
    }

    public PrismContext prismContext() {
        return sqlRepoContext.prismContext();
    }

    public <T> Class<? extends T> qNameToSchemaClass(@NotNull QName qName) {
        return sqlRepoContext.qNameToSchemaClass(qName);
    }

    public CanonicalItemPath createCanonicalItemPath(@NotNull ItemPath itemPath) {
        return sqlRepoContext.prismContext().createCanonicalItemPath(itemPath);
    }

    @NotNull
    public QName normalizeRelation(QName qName) {
        return sqlRepoContext.normalizeRelation(qName);
    }

    /**
     * Before-query hook, empty by default, called *before* the JDBC transaction starts.
     */
    public void beforeQuery() {
    }

    /**
     * Produces predicate for fuzzy filter with pre-provided expression for the left side.
     * This does not care about single/multi-value definition which must be treated above this method.
     */
    public Predicate processFuzzyFilter(
            FuzzyStringMatchFilter<?> filter, Expression<?> path, ValueFilterValues<?, ?> values)
            throws QueryException {
        throw new QueryException("Unsupported filter " + filter.toString());
    }
}
