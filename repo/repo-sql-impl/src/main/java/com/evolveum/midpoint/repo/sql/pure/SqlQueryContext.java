package com.evolveum.midpoint.repo.sql.pure;

import static com.evolveum.midpoint.repo.sql.pure.SqlQueryExecutor.QUERYDSL_CONFIGURATION;

import java.sql.Connection;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Context information about SQL query.
 * Works as a kind of accumulator where information are added as the object query is interpreted.
 */
public class SqlQueryContext extends SqlPathContext implements FilterProcessor<ObjectFilter> {

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
        Predicate condition = new ObjectFilterProcessor(this).process(filter);
        query.where(condition);
        // probably not used after added to where, but let's respect the contract
        return condition;
    }
}
