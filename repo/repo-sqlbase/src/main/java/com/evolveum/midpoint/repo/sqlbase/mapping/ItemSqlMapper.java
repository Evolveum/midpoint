/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.mapping;

import com.querydsl.core.types.Expression;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.RightHandProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Defines contract for mapping component holding information how an item (from schema/prism world)
 * is to be processed when interpreting query.
 *
 * @param <Q> entity path owning the mapped item
 * @param <R> row type with the mapped item
 */
public interface ItemSqlMapper<Q extends FlexibleRelationalPathBase<R>, R> {

    /** Returns primary path for provided entity path - usable for ordering. */
    @Nullable Expression<?> primaryPath(Q entityPath, ItemDefinition<?> definition)
            throws QueryException;

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
     * but not filtering for queries (e.g. update only item).
     */
    @Nullable <T extends ValueFilter<?, ?>> ItemValueFilterProcessor<T> createFilterProcessor(
            SqlQueryContext<?, ?, ?> sqlQueryContext);

    @Nullable RightHandProcessor createRightHandProcessor(
            SqlQueryContext<?, ?, ?> sqlQueryContext);

}
