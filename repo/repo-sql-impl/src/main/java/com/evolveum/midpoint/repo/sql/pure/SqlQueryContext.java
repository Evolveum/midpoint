package com.evolveum.midpoint.repo.sql.pure;

import static com.evolveum.midpoint.repo.sql.pure.SqlQueryExecutor.QUERYDSL_CONFIGURATION;

import java.sql.Connection;
import java.util.List;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.pure.mapping.SqlDetailFetchMapper;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Context information about SQL query.
 * Works as a kind of accumulator where information are added as the object query is interpreted.
 * It is also used as an entry point for {@link FilterProcessor} execution for this query.
 */
public class SqlQueryContext<Q extends EntityPath<R>, R> extends SqlPathContext<Q, R>
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

    private final SQLQuery<?> query;

    public SqlQueryContext(QueryModelMapping<?, Q, R> rootMapping, PrismContext prismContext)
            throws QueryException {
        super(rootMapping.defaultAlias(), rootMapping, prismContext);
        query = new SQLQuery<>(QUERYDSL_CONFIGURATION).from(root());
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
        query.where(condition);
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
            query.offset(offset.longValue());
            Integer pageSize = paging.getMaxSize();
            query.limit(pageSize != null ? pageSize.longValue() : DEFAULT_PAGE_SIZE);
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
                query.orderBy(((ComparableExpressionBase<?>) path).desc());
            } else {
                query.orderBy(((ComparableExpressionBase<?>) path).asc());
            }
        }
    }

    public void setDistinct(boolean distinct) {
        if (distinct) {
            query.distinct();
        }
    }

    public SQLQuery<?> newQuery(Connection conn) {
        return new SQLQuery<>(conn, QUERYDSL_CONFIGURATION);
    }

    public PageOf<R> executeQuery(Connection conn) {
        SQLQuery<R> query = this.query.clone(conn)
                .select(root());
        if (query.getMetadata().getModifiers().getLimit() == null) {
            query.limit(NO_PAGINATION_LIMIT);
        }
        // TODO logging
        System.out.println("SQL query = " + query);

        long count = query.fetchCount();
        List<R> data = query.fetch();

        for (SqlDetailFetchMapper<R, ?, ?, ?> fetcher : mapping().detailFetchMappers()) {
            fetcher.execute(() -> newQuery(conn), data);
        }

        return new PageOf<>(data, PageOf.PAGE_NO_PAGINATION, 0, count);
    }
}
