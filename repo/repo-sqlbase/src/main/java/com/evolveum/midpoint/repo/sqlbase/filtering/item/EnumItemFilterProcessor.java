/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import java.util.function.Function;

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EnumPath;

import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.RightHandProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.ValueFilterValues;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;

/**
 * Filter processor for an attribute path (Prism item) of enum type that is mapped
 * to matching PostgreSQL enum type - this allows to use schema enums directly.
 * Use only enums that change rarely-to-never, enum type defined in SQL schema must be changed
 * accordingly - but this is still less complicated than with old repo where each enum was doubled.
 *
 * Each enum type must be registered in {@link QuerydslUtils#querydslConfiguration}.
 */
public class EnumItemFilterProcessor<E extends Enum<E>>
        extends SinglePathItemFilterProcessor<E, EnumPath<E>> {

    public <S, Q extends FlexibleRelationalPathBase<R>, R> EnumItemFilterProcessor(
            SqlQueryContext<S, Q, R> context,
            Function<Q, EnumPath<E>> rootToQueryItem) {
        super(context, rootToQueryItem);
    }

    @Override
    public Predicate process(PropertyValueFilter<E> filter) throws QueryException {
        return createBinaryCondition(filter, path, ValueFilterValues.from(filter));
    }


    @Override
    public Predicate process(PropertyValueFilter<E> filter, RightHandProcessor rightPath)
            throws RepositoryException {
        return createBinaryCondition(filter, path,
                ValueFilterValues.from(filter, rightPath.rightHand(filter)));
    }

    @Override
    protected FilterOperation operation(ValueFilter<?, ?> filter) throws QueryException {
        if (filter instanceof EqualFilter && filter.getMatchingRule() == null) {
            return FilterOperation.of(Ops.EQ);
        } else {
            throw new QueryException("Can't translate filter '" + filter + "' to operation."
                    + " Enumeration value supports only equals with no matching rule.");
        }
    }
}
