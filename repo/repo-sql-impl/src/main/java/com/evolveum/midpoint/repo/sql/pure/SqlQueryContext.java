package com.evolveum.midpoint.repo.sql.pure;

import static com.evolveum.midpoint.repo.sql.pure.SqlQueryExecutor.QUERYDSL_CONFIGURATION;

import java.sql.Connection;
import java.util.List;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Context information about SQL query.
 * Works as a kind of accumulator where information are added as the object query is interpreted.
 * It is also used as an entry point for {@link FilterProcessor} execution for this query.
 */
public class SqlQueryContext extends SqlPathContext implements FilterProcessor<ObjectFilter> {

    public static final int DEFAULT_PAGE_SIZE = 10;

    private final SQLQuery<?> query;

    public SqlQueryContext(
            EntityPath<?> root,
            QueryModelMapping<?, ?> rootMapping) {
        super(root, rootMapping);
        query = new SQLQuery<>(QUERYDSL_CONFIGURATION).from(root);
    }

    public EntityPath<?> root() {
        return path();
    }

    public <T extends FlexibleRelationalPathBase<?>> T root(Class<T> rootType) {
        return path(rootType);
    }

    public SQLQuery<?> query(Connection connection) {
        return query.clone(connection);
    }

    public void addPredicate(Predicate predicate) {
        query.where(predicate);
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
}
