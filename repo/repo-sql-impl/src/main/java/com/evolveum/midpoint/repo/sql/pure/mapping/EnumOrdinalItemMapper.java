package com.evolveum.midpoint.repo.sql.pure.mapping;

import com.querydsl.core.types.*;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.pure.FilterProcessor;
import com.evolveum.midpoint.repo.sql.query.QueryException;

/**
 * Mapping for a an attribute path (Prism item) of enum type that is mapped to SQL as ordinal value.
 * Actual query-path is determined by the function from a parent path (entity).
 */
public class EnumOrdinalItemMapper implements FilterProcessor<PropertyValueFilter<Enum<?>>> {

    private final Path<?> path;

    /**
     * Creates enum filter processor for concrete attribute path.
     */
    public EnumOrdinalItemMapper(Path<?> path) {
        this.path = path;
    }

    @Override
    public Predicate process(PropertyValueFilter<Enum<?>> filter) throws QueryException {
        //noinspection ConstantConditions
        Enum<?> singleValue = filter.getSingleValue().getRealValue();
        Ops operator = operation(filter);
        return ExpressionUtils.predicate(operator, path,
                ConstantImpl.create(singleValue.ordinal()));
    }
}
