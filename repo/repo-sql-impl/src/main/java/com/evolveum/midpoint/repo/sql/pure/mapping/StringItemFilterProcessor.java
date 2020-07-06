package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.pure.SqlPathContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Filter processor for a string attribute path (Prism item).
 */
public class StringItemFilterProcessor extends ItemFilterProcessor<PropertyValueFilter<String>> {

    private final Path<?> path;

    /**
     * Returns the mapper function creating the string filter processor from context.
     */
    public static Function<SqlPathContext, FilterProcessor<?>> mapper(
            Function<EntityPath<?>, Path<?>> rootToQueryItem) {
        return context -> new StringItemFilterProcessor(rootToQueryItem.apply(context.path()));
    }

    private StringItemFilterProcessor(Path<?> path) {
        this.path = path;
    }

    @Override
    public Predicate process(PropertyValueFilter<String> filter) throws QueryException {
        String value = getSingleValue(filter);
        return createBinaryCondition(filter, path, value);
    }
}
