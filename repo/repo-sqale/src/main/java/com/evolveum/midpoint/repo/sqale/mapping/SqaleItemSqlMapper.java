/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.Objects;
import java.util.function.Function;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.DefaultItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Declarative information how an item (from schema/prism world) is to be processed
 * when interpreting query or applying delta (delta application is addition to sqlbase superclass).
 * Being extension of {@link DefaultItemSqlMapper} this uses processor factory functions
 * (typically provided as lambdas), no logic is here, everything is delegated to the processors
 * returned by these processor functions.
 *
 * @param <S> schema type owning the mapped item
 * @param <Q> entity path owning the mapped item
 * @param <R> row type with the mapped item
 */
public class SqaleItemSqlMapper<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends DefaultItemSqlMapper<S, Q, R> implements UpdatableItemSqlMapper<Q, R> {

    @NotNull private final
    Function<SqaleUpdateContext<S, Q, R>, ItemDeltaValueProcessor<?>> deltaProcessorFactory;

    public <P extends Path<?>> SqaleItemSqlMapper(
            @NotNull Function<SqlQueryContext<S, Q, R>, ItemValueFilterProcessor<?>> filterProcessorFactory,
            @NotNull Function<SqaleUpdateContext<S, Q, R>, ItemDeltaValueProcessor<?>> deltaProcessorFactory,
            @Nullable Function<Q, P> primaryItemMapping) {
        super(filterProcessorFactory, primaryItemMapping);
        this.deltaProcessorFactory = Objects.requireNonNull(deltaProcessorFactory);
    }

    public SqaleItemSqlMapper(
            @NotNull Function<SqlQueryContext<S, Q, R>, ItemValueFilterProcessor<?>> filterProcessorFactory,
            @NotNull Function<SqaleUpdateContext<S, Q, R>, ItemDeltaValueProcessor<?>> deltaProcessorFactory) {
        super(filterProcessorFactory);
        this.deltaProcessorFactory = Objects.requireNonNull(deltaProcessorFactory);
    }

    /**
     * Version of mapper supporting only delta processor, but not filter processor.
     * Typical example is container which can't be used as a filter condition as a whole.
     * This does not mean the inner part of the item can't be resolved and filtered by, but
     * it's not job of this mapper (see {@link ItemRelationResolver}).
     */
    public SqaleItemSqlMapper(
            @NotNull Function<SqaleUpdateContext<S, Q, R>, ItemDeltaValueProcessor<?>> deltaProcessorFactory) {
        super(ctx -> new ItemValueFilterProcessor<>(ctx) {
            @Override
            public Predicate process(ValueFilter<?, ?> filter) throws RepositoryException {
                throw new QueryException(
                        "Value filter processor not supported for filter: " + filter);
            }
        });

        this.deltaProcessorFactory = Objects.requireNonNull(deltaProcessorFactory);
    }

    /**
     * Creates {@link ItemDeltaProcessor} based on this mapping.
     * Provided {@link SqaleUpdateContext} is used to figure out the query paths when this is
     * executed (as the entity path instance is not yet available when the mapping is configured
     * in a declarative manner).
     *
     * The type of the returned processor is adapted to the client code needs for convenience.
     * Also the type of the provided context is flexible, but with proper mapping it's all safe.
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ItemDeltaValueProcessor<?> createItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> sqlUpdateContext) {
        return deltaProcessorFactory.apply((SqaleUpdateContext) sqlUpdateContext);
    }
}
