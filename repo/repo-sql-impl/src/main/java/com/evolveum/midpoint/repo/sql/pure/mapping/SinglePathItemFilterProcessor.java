package com.evolveum.midpoint.repo.sql.pure.mapping;

import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.pure.SqlPathContext;

/**
 * Typical item filter processor is related to one table column represented by the {@link #path}.
 * This is typically obtained from context path (typically relational) using mapping function.
 * Typically it's the same function that is also called "primary mapping" and used for ordering.
 */
public abstract class SinglePathItemFilterProcessor<O extends ObjectFilter>
        extends ItemFilterProcessor<O> {

    protected final Path<?> path;

    public SinglePathItemFilterProcessor(
            SqlPathContext<?, ?> context, Function<EntityPath<?>, Path<?>> rootToQueryItem) {
        super(context);
        this.path = rootToQueryItem.apply(context.path());
    }
}
