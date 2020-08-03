package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.SqlPathContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Filter processor for a string attribute path (Prism item).
 */
public class StringItemFilterProcessor
        extends SinglePathItemFilterProcessor<PropertyValueFilter<String>> {

    /**
     * Returns the mapper creating the string filter processor from context.
     */
    public static ItemSqlMapper mapper(
            Function<EntityPath<?>, Path<?>> rootToQueryItem) {
        return new ItemSqlMapper(ctx ->
                new StringItemFilterProcessor(ctx, rootToQueryItem), rootToQueryItem);
    }

    private StringItemFilterProcessor(
            SqlPathContext<?, ?, ?> context, Function<EntityPath<?>, Path<?>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<String> filter) throws QueryException {
        return createBinaryCondition(filter, path, new ValueFilterValues<>(filter));
    }
}
