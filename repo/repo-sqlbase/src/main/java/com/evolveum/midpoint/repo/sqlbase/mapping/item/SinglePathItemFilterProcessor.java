/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping.item;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;

/**
 * Typical item filter processor is related to one table column represented by the {@link #path}.
 * This is typically obtained from context path (typically relational) using mapping function.
 * Typically it's the same function that is also called "primary mapping" and used for ordering.
 */
public abstract class SinglePathItemFilterProcessor<O extends ObjectFilter, P extends Path<?>>
        extends ItemFilterProcessor<O> {

    protected final P path;

    public SinglePathItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context, Function<EntityPath<?>, P> rootToQueryItem) {
        super(context);
        this.path = rootToQueryItem.apply(context.path());
    }
}
