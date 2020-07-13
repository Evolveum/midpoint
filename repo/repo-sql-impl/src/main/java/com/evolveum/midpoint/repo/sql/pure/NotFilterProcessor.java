package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;

public class NotFilterProcessor implements FilterProcessor<NotFilter> {

    private final SqlPathContext<?, ?> context;

    public NotFilterProcessor(SqlPathContext<?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(NotFilter filter) throws QueryException {
        context.markNotFilterUsage();
        return new ObjectFilterProcessor(context)
                .process(filter.getConditions().get(0))
                .not();
    }
}
