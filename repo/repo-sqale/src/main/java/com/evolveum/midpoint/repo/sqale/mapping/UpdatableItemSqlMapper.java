/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Extension of {@link ItemSqlMapper} adding update capability (delta processing).
 *
 * @param <Q> entity path owning the mapped item
 * @param <R> row type with the mapped item
 */
public interface UpdatableItemSqlMapper<Q extends FlexibleRelationalPathBase<R>, R>
        extends ItemSqlMapper<Q, R> {

    /**
     * Creates {@link ItemDeltaProcessor} based on this mapping.
     * Provided {@link SqaleUpdateContext} is used to figure out the query paths when this is
     * executed (as the entity path instance is not yet available when the mapping is configured
     * in a declarative manner).
     *
     * The type of the returned processor is adapted to the client code needs for convenience.
     * Also the type of the provided context is flexible, but with proper mapping it's all safe.
     */
    ItemDeltaProcessor createItemDeltaProcessor(SqaleUpdateContext<?, ?, ?> sqlUpdateContext);
}
