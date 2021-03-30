/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;

public class ObjectFilterProcessor implements FilterProcessor<ObjectFilter> {

    private final SqlQueryContext<?, ?, ?> context;

    public ObjectFilterProcessor(SqlQueryContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(ObjectFilter filter) throws RepositoryException {
        if (filter instanceof NaryLogicalFilter) {
            return new NaryLogicalFilterProcessor(context)
                    .process((NaryLogicalFilter) filter);
        } else if (filter instanceof NotFilter) {
            return new NotFilterProcessor(context)
                    .process((NotFilter) filter);
        } else if (filter instanceof ValueFilter) {
            // here are the values applied (ref/property value filters)
            return new ValueFilterProcessor(context)
                    .process((ValueFilter<?, ?>) filter);
// TODO see QueryInterpreter.findAndCreateRestrictionInternal for uncovered cases
//  } else if (filter instanceof OrgFilter) {
        } else if (filter instanceof InOidFilter) {
            return context.createInOidFilter(context)
                    .process((InOidFilter) filter);
        } else if (filter instanceof AllFilter) {
            return Expressions.asBoolean(true).isTrue();
        } else if (filter instanceof NoneFilter) {
            return Expressions.asBoolean(true).isFalse();
        } else {
            throw new QueryException("Unsupported filter " + filter);
        }
    }
}
