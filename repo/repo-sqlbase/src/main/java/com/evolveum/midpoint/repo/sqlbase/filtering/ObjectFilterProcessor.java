/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlPathContext;

public class ObjectFilterProcessor implements FilterProcessor<ObjectFilter> {

    private final SqlPathContext<?, ?, ?> context;

    public ObjectFilterProcessor(SqlPathContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(ObjectFilter filter) throws QueryException {
        if (filter instanceof NaryLogicalFilter) {
            return new NaryLogicalFilterProcessor(context)
                    .process((NaryLogicalFilter) filter);
        } else if (filter instanceof NotFilter) {
            return new NotFilterProcessor(context)
                    .process((NotFilter) filter);
        } else if (filter instanceof PropertyValueFilter) {
            return new PropertyValueFilterProcessor(context)
                    .process((PropertyValueFilter<?>) filter);
        } else if (filter instanceof RefFilter) {
            return new RefFilterProcessor(context)
                    .process((RefFilter) filter);
        } else if (filter instanceof AllFilter) {
            return Expressions.asBoolean(true).isTrue();
        } else if (filter instanceof NoneFilter) {
            return Expressions.asBoolean(true).isFalse();
        } else {
            throw new QueryException("Unsupported filter " + filter);
        }
    }
}
