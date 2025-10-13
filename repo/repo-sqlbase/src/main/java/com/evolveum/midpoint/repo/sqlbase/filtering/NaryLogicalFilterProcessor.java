/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;

public class NaryLogicalFilterProcessor implements FilterProcessor<NaryLogicalFilter> {

    private final SqlQueryContext<?, ?, ?> context;

    public NaryLogicalFilterProcessor(SqlQueryContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(NaryLogicalFilter filter) throws RepositoryException {
        Predicate predicate = null;
        Operator operator = (filter instanceof AndFilter) ? Ops.AND : Ops.OR;
        for (ObjectFilter subfilter : filter.getConditions()) {
            Predicate right = context.process(subfilter);
            if (right != null) {
                predicate = predicate != null
                        ? ExpressionUtils.predicate(operator, predicate, right)
                        : right;
            }
        }

        return predicate;
    }
}
