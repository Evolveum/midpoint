package com.evolveum.midpoint.repo.sql.pure.mapping;

import com.querydsl.core.types.*;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Mapping for a string attribute path (Prism item).
 */
public class StringItemMapper implements FilterProcessor<PropertyValueFilter<String>> {

    private final Path<?> path;

    public StringItemMapper(Path<?> path) {
        this.path = path;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Predicate process(PropertyValueFilter<String> filter) throws QueryException {
        String value = filter.getSingleValue().getRealValue();
        Ops operator = operation(filter);
        return ExpressionUtils.predicate(operator, path, ConstantImpl.create(value));
    }
}
