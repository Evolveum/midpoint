/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

import java.util.Objects;
import java.util.function.Function;

import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.RightHandProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

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
 *
 * @param <S> schema type owning the mapped item
 * @param <Q> entity path owning the mapped item
 * @param <R> row type with the mapped item
 */
public class DefaultItemSqlMapper<S, Q extends FlexibleRelationalPathBase<R>, R>
        implements ItemSqlMapper<Q, R> {

    /**
     * Primary mapping is used for order by clauses (if they are comparable).
     * Mappers can map to multiple query attributes (e.g. poly-string has orig and norm),
     * so normally the mapping(s) are encapsulated there, but for order we need one exposed.
     * Can be {@code null} which indicates that ordering is not possible.
     */
    @Nullable private final Function<Q, Path<?>> primaryItemMapping;

    @NotNull private final
    Function<SqlQueryContext<S, Q, R>, ItemValueFilterProcessor<?>> filterProcessorFactory;

    public <P extends Path<?>> DefaultItemSqlMapper(
            @NotNull Function<SqlQueryContext<S, Q, R>, ItemValueFilterProcessor<?>> filterProcessorFactory,
            @Nullable Function<Q, P> primaryItemMapping) {
        this.filterProcessorFactory = Objects.requireNonNull(filterProcessorFactory);
        //noinspection unchecked
        this.primaryItemMapping = (Function<Q, Path<?>>) primaryItemMapping;
    }

    public DefaultItemSqlMapper(
            @NotNull Function<SqlQueryContext<S, Q, R>, ItemValueFilterProcessor<?>> filterProcessorFactory) {
        this(filterProcessorFactory, null);
    }

    @Override
    public @Nullable Path<?> primaryPath(Q root, ItemDefinition<?> unused) {
        return primaryItemMapping != null ? primaryItemMapping.apply(root) : null;
    }

    /**
     * Creates {@link ItemValueFilterProcessor} based on this mapping.
     * Provided {@link SqlQueryContext} is used to figure out the query paths when this is executed
     * (as the entity path instance is not yet available when the mapping is configured
     * in a declarative manner).
     *
     * The type of the returned processor is adapted to the client code needs for convenience.
     * Also the type of the provided context is flexible, but with proper mapping it's all safe.
     *
     * [NOTE]
     * This may return null if the subclass supports other type of mapping for this item,
     * but not filtering for queries.
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public @Nullable <T extends ValueFilter<?, ?>> ItemValueFilterProcessor<T> createFilterProcessor(
            SqlQueryContext<?, ?, ?> sqlQueryContext) {
        return filterProcessorFactory.apply((SqlQueryContext) sqlQueryContext);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public @Nullable RightHandProcessor createRightHandProcessor(
            SqlQueryContext<?, ?, ?> sqlQueryContext) {
        return filterProcessorFactory.apply((SqlQueryContext) sqlQueryContext);
    }
}
