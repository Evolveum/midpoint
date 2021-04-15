/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;

/**
 * Filter processor for a single path with straightforward type mapping and no conversions.
 */
public class SimpleItemFilterProcessor<T, P extends Path<T>>
        extends SinglePathItemFilterProcessor<PropertyValueFilter<T>, P> {

    public SimpleItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context, Function<EntityPath<?>, P> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<T> filter) throws QueryException {
        return createBinaryCondition(filter, path, ValueFilterValues.from(filter));
    }
}
