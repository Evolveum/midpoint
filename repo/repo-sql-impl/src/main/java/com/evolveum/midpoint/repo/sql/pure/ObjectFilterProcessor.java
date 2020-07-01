package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;

public class ObjectFilterProcessor implements FilterProcessor<ObjectFilter> {

    private final SqlPathContext context;

    public ObjectFilterProcessor(SqlPathContext context) {
        this.context = context;
    }

    @Override
    public Predicate process(ObjectFilter filter) throws QueryException {
        if (filter instanceof PropertyValueFilter) {
            return new PropertyValueFilterProcessor(context)
                    .process((PropertyValueFilter<?>) filter);
        } else {
            throw new QueryException("Unsupported filter " + filter);
        }
    }
}
