/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import java.util.function.Function;

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberPath;

import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.FilterOperation;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SinglePathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Filter processor for URI item paths - represented by string/QName in schema and by int ID in DB.
 * These paths are generally not ordered by, which is a relief, otherwise JOIN would be needed.
 */
public class UriItemFilterProcessor
        extends SinglePathItemFilterProcessor<Object, NumberPath<Integer>> {

    public <Q extends FlexibleRelationalPathBase<R>, R> UriItemFilterProcessor(
            SqlQueryContext<?, Q, R> context,
            Function<Q, NumberPath<Integer>> rootToPath) {
        super(context, rootToPath);
    }

    @Override
    public Predicate process(PropertyValueFilter<Object> filter) throws QueryException {
        return createBinaryCondition(filter, path,
                ValueFilterValues.from(filter,
                        ((SqaleRepoContext) context.repositoryContext())::searchCachedUriId));
    }

    @Override
    protected FilterOperation operation(ValueFilter<?, ?> filter) throws QueryException {
        if (filter instanceof EqualFilter && filter.getMatchingRule() == null) {
            return FilterOperation.of(Ops.EQ);
        } else {
            throw new QueryException("Can't translate filter '" + filter + "' to operation."
                    + " URI/QName value supports only equals with no matching rule.");
        }
    }
}
