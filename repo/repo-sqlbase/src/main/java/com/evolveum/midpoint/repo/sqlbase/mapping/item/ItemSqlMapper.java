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
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemFilterProcessor;

/**
 * Declarative information how an item (from schema/prism world) is to be processed
 * when interpreting query.
 * As this is declarative it does not point to any Q-class attributes - instead it knows
 * how to get to the attributes when the Q-class instance (entity path) is provided; this is
 * provided as a function (or functions for multiple paths), typically as lambdas.
 *
 * Based on this information the mapper can later create {@link FilterProcessor} when needed,
 * again providing the right type of {@link FilterProcessor}, based on the type of the item
 * and/or how the item is mapped to the database.
 */
public class ItemSqlMapper {

    /**
     * Primary mapping is used for order by clauses (if they are comparable).
     * Mappers can map to multiple query attributes (e.g. poly-string has orig and norm),
     * so normally the mapping(s) are encapsulated there, but for order we need one exposed.
     * Can be {@code null} which indicates that ordering is not possible.
     */
    @Nullable private final Function<EntityPath<?>, Path<?>> primaryItemMapping;

    @NotNull private final
    Function<SqlQueryContext<?, ?, ?>, ItemFilterProcessor<?>> filterProcessorFactory;

    public <P extends Path<?>> ItemSqlMapper(
            @NotNull Function<SqlQueryContext<?, ?, ?>, ItemFilterProcessor<?>> filterProcessorFactory,
            @Nullable Function<EntityPath<?>, P> primaryItemMapping) {
        this.filterProcessorFactory = Objects.requireNonNull(filterProcessorFactory);
        //noinspection unchecked
        this.primaryItemMapping = (Function<EntityPath<?>, Path<?>>) primaryItemMapping;
    }

    public ItemSqlMapper(
            @NotNull Function<SqlQueryContext<?, ?, ?>, ItemFilterProcessor<?>> filterProcessorFactory) {
        this(filterProcessorFactory, null);
    }

    public @Nullable Path<?> itemPrimaryPath(EntityPath<?> root) {
        return primaryItemMapping != null ? primaryItemMapping.apply(root) : null;
    }

    /**
     * Creates {@link ItemFilterProcessor} based on this mapping.
     * Provided {@link SqlQueryContext} is used to figure out the query paths when this is executed
     * (as the entity path instance is not yet available when the mapping is configured
     * in a declarative manner).
     *
     * The type of the returned filter is adapted to the client code needs for convenience.
     */
    public <T extends ObjectFilter> ItemFilterProcessor<T> createFilterProcessor(
            SqlQueryContext<?, ?, ?> sqlQueryContext) {
        //noinspection unchecked
        return (ItemFilterProcessor<T>) filterProcessorFactory.apply(sqlQueryContext);
    }
}
