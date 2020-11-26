/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlPathContext;

public class NotFilterProcessor implements FilterProcessor<NotFilter> {

    private final SqlPathContext<?, ?, ?> context;

    public NotFilterProcessor(SqlPathContext<?, ?, ?> context) {
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
