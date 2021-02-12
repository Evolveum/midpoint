/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping.item;

import java.util.Objects;
import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;

/**
 * Declarative information how an item (from schema/prism world) is to be processed
 * when interpreting query.
 * As this is declarative it does not point to any Q-class attributes - instead it knows
 * how to get to the attributes when the Q-class instance (entity path) is provided; this is
 * provided as a function (or functions for multiple paths), typically as lambdas.
 *
 * Based on this information the mapper can also create {@link FilterProcessor} when needed,
 * again providing the right type of {@link FilterProcessor} via a function, based on the type
 * of item and/or how the item is mapped to the database.
 */
public class ItemSqlMapper {

    /**
     * Primary mapping is used for order by clauses (if they are comparable).
     * Mappers can map to multiple query attributes (e.g. poly-string has orig and norm),
     * so normally the mapping(s) are encapsulated there, but for order we need one exposed.
     * Can be {@code null} which indicates that ordering is not possible.
     */
    @Nullable private final Function<EntityPath<?>, Path<?>> primaryItemMapping;

    @NotNull private final Function<SqlQueryContext<?, ?, ?>, FilterProcessor<?>> filterProcessorFactory;

    public <P extends Path<?>> ItemSqlMapper(
            @NotNull Function<SqlQueryContext<?, ?, ?>, FilterProcessor<?>> filterProcessorFactory,
            @Nullable Function<EntityPath<?>, P> primaryItemMapping) {
        this.filterProcessorFactory = Objects.requireNonNull(filterProcessorFactory);
        //noinspection unchecked
        this.primaryItemMapping = (Function<EntityPath<?>, Path<?>>) primaryItemMapping;
    }

    public ItemSqlMapper(
            @NotNull Function<SqlQueryContext<?, ?, ?>, FilterProcessor<?>> filterProcessorFactory) {
        this(filterProcessorFactory, null);
    }

    public @Nullable Path<?> itemPrimaryPath(EntityPath<?> root) {
        return primaryItemMapping != null ? primaryItemMapping.apply(root) : null;
    }

    public <T extends ObjectFilter> FilterProcessor<T> createFilterProcessor(
            SqlQueryContext<?, ?, ?> pathContext) {
        //noinspection unchecked
        return (FilterProcessor<T>) filterProcessorFactory.apply(pathContext);
    }

    // TODO createDeltaProcessor?
}
