package com.evolveum.midpoint.repo.sql.pure;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;

public class NaryLogicalFilterProcessor implements FilterProcessor<NaryLogicalFilter> {

    private final SqlPathContext<?, ?, ?> context;

    public NaryLogicalFilterProcessor(SqlPathContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(NaryLogicalFilter filter) throws QueryException {
        Predicate predicate = null;
        Operator operator = (filter instanceof AndFilter) ? Ops.AND : Ops.OR;
        for (ObjectFilter subfilter : filter.getConditions()) {
            Predicate right = new ObjectFilterProcessor(context).process(subfilter);
            predicate = predicate != null
                    ? ExpressionUtils.predicate(operator, predicate, right)
                    : right;
        }

        return predicate;
    }
}
