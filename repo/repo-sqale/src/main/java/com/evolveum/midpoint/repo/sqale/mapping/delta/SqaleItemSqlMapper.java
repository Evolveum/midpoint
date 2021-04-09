/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping.delta;

import java.util.Objects;
import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;

/**
 * Declarative information how an item (from schema/prism world) is to be processed
 * when interpreting query or applying delta (delta application is addition to sqlbase superclass).
 */
public class SqaleItemSqlMapper extends ItemSqlMapper {

    @NotNull private final
    Function<SqaleUpdateContext<?, ?, ?>, ItemDeltaProcessor<?>> deltaProcessorFactory;

    public <P extends Path<?>> SqaleItemSqlMapper(
            @NotNull Function<SqlQueryContext<?, ?, ?>, ItemFilterProcessor<?>> filterProcessorFactory,
            @NotNull Function<SqaleUpdateContext<?, ?, ?>, ItemDeltaProcessor<?>> deltaProcessorFactory,
            @Nullable Function<EntityPath<?>, P> primaryItemMapping) {
        super(filterProcessorFactory, primaryItemMapping);
        this.deltaProcessorFactory = Objects.requireNonNull(deltaProcessorFactory);
    }

    public SqaleItemSqlMapper(
            @NotNull Function<SqlQueryContext<?, ?, ?>, ItemFilterProcessor<?>> filterProcessorFactory,
            @NotNull Function<SqaleUpdateContext<?, ?, ?>, ItemDeltaProcessor<?>> deltaProcessorFactory) {
        super(filterProcessorFactory);
        this.deltaProcessorFactory = Objects.requireNonNull(deltaProcessorFactory);
    }

    /**
     * Creates {@link ItemDeltaProcessor} based on this mapping.
     * Provided {@link SqaleUpdateContext} is used to figure out the query paths when this is
     * executed (as the entity path instance is not yet available when the mapping is configured
     * in a declarative manner).
     *
     * The type of the returned filter is adapted to the client code needs for convenience.
     */
    public ItemDeltaProcessor<?> createItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> sqlUpdateContext) {
        return deltaProcessorFactory.apply(sqlUpdateContext);
    }
}
