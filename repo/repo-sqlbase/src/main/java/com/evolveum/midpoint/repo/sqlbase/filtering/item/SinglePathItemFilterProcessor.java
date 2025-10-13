/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqlbase.filtering.item;

import java.util.function.Function;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Item filter processor related to one table column represented by the {@link #path}.
 * The path is typically obtained from query context using the provided mapping function.
 * The same function is also called "primary mapping" and used for ordering (if possible).
 * Single-path does not mean single value - although normally used for single-value properties,
 * multi-values can be represented in DB by array or JSOBN columns too.
 *
 * @param <T> type parameter of processed {@link PropertyValueFilter}
 * @param <P> type of the Querydsl path
 */
public abstract class SinglePathItemFilterProcessor<T, P extends Path<?>>
        extends ItemValueFilterProcessor<PropertyValueFilter<T>> {

    protected final P path;

    public <Q extends FlexibleRelationalPathBase<R>, R> SinglePathItemFilterProcessor(
            SqlQueryContext<?, Q, R> context, Function<Q, P> rootToQueryItem) {
        super(context);
        this.path = rootToQueryItem.apply(context.path());
    }

    @Override
    public Expression<?> rightHand(ValueFilter<?, ?> filter) throws RepositoryException {
        return path;
    }

    public P getPath() {
        return path;
    }
}
