package com.evolveum.midpoint.repo.sql.pure;

import static com.evolveum.midpoint.repo.sql.pure.SqlQueryExecutor.QUERYDSL_CONFIGURATION;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMappingConfig;
import com.evolveum.midpoint.repo.sql.pure.mapping.SqlDetailFetchMapper;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

/**
 * Context information about SQL query.
 * Works as a kind of accumulator where information are added as the object query is interpreted.
 * It is also used as an entry point for {@link FilterProcessor} execution for this query.
 *
 * @param <S> schema type, used by encapsulated mapping
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class SqlQueryContext<S, Q extends EntityPath<R>, R> extends SqlPathContext<S, Q, R>
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

    private final SQLQuery<?> sqlQuery;

    public static <S, Q extends EntityPath<R>, R> SqlQueryContext<S, Q, R> from(
            Class<S> schemaType, PrismContext prismContext) throws QueryException {

        QueryModelMapping<S, Q, R> rootMapping = QueryModelMappingConfig.getBySchemaType(schemaType);
        return new SqlQueryContext<>(rootMapping, prismContext);
    }

    private SqlQueryContext(QueryModelMapping<S, Q, R> rootMapping, PrismContext prismContext)
            throws QueryException {
        super(rootMapping.defaultAlias(), rootMapping, prismContext);
        sqlQuery = new SQLQuery<>(QUERYDSL_CONFIGURATION).from(root());
    }

    // private constructor for "derived" query contexts
    private SqlQueryContext(
            Q defaultAlias, QueryModelMapping<S, Q, R> mapping, PrismContext prismContext, SQLQuery<?> query) {
        super(defaultAlias, mapping, prismContext);
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
     * This takes care of {@link ObjectPaging}
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

    public SQLQuery<?> newQuery(Connection conn) {
        return new SQLQuery<>(conn, QUERYDSL_CONFIGURATION);
    }

    public PageOf<R> executeQuery(Connection conn) throws QueryException {
        SQLQuery<R> query = this.sqlQuery.clone(conn)
                .select(root());
        if (query.getMetadata().getModifiers().getLimit() == null) {
            query.limit(NO_PAGINATION_LIMIT);
        }
        // TODO MID-6319: logging
        System.out.println("SQL query: " + query);

        // TODO MID-6319: make this optional (based on options)? We have explicit count now.
        long count = query.fetchCount();
        List<R> data = query.fetch();

        for (SqlDetailFetchMapper<R, ?, ?, ?> fetcher : mapping().detailFetchMappers()) {
            fetcher.execute(() -> newQuery(conn), data);
        }

        return new PageOf<>(data, PageOf.PAGE_NO_PAGINATION, 0, count);
    }

    public int executeCount(Connection conn) {
        return (int) sqlQuery.clone(conn)
                .select(root())
                .fetchCount();
    }

    /**
     * Adds new LEFT JOIN to the query and returns {@link SqlQueryContext} for this join path.
     *
     * @param <DQ> query type for the JOINed table
     * @param <DR> row type related to the {@link DQ}
     * @param newPath entity path representing the JOIN (must be pre-created with unique alias)
     * @param joinOnPredicateFunction bi-function producing ON predicate for the JOIN
     */
    @Override
    public <DQ extends EntityPath<DR>, DR> SqlQueryContext<?, DQ, DR> leftJoin(
            @NotNull DQ newPath,
            @NotNull BiFunction<Q, DQ, Predicate> joinOnPredicateFunction) {
        sqlQuery.leftJoin(newPath).on(joinOnPredicateFunction.apply(path(), newPath));

        // getClass returns Class<?> but it is really Class<DQ>, just suppressing the warning here
        //noinspection unchecked
        QueryModelMapping<?, DQ, DR> mapping =
                QueryModelMappingConfig.getByQueryType(newPath.getClass());

        return new SqlQueryContext<>(newPath, mapping, prismContext(), sqlQuery);
    }

    @Override
    public String uniqueAliasName(String baseAliasName) {
        // TODO MID-6319 inspect query and resolve alias name
        return baseAliasName;
    }

    public void processOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        if (options == null || options.isEmpty()) {
            return;
        }

        // TODO MID-6319: what other options are here? can they all be processed after filter?
        if (GetOperationOptions.isDistinct(SelectorOptions.findRootOptions(options))) {
            sqlQuery.distinct();
        }
    }

    public PageOf<S> transformToSchemaType(PageOf<R> result) {
        SqlTransformer<S, R> transformer = mapping().createTransformer(prismContext());
        return result.map(transformer::toSchemaObject);
    }
}
