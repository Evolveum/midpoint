/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Resolver for indexed extension/attributes containers.
 *
 * @param <S> schema type for the extension/attributes container
 * @param <Q> query type of entity where the mapping is declared
 * @param <R> row type of {@link Q}
 */
public class ExtensionMappingResolver<S extends Containerable, Q extends FlexibleRelationalPathBase<R>, R>
        implements SqaleItemRelationResolver<Q, R> {

    private final ExtensionMapping<S, Q, R> mapping;
    private final ItemName itemName; // name of the extension container // TODO needed?

    public ExtensionMappingResolver(
            @NotNull ExtensionMapping<S, Q, R> mapping,
            @NotNull ItemName itemName) {
        this.mapping = mapping;
        this.itemName = itemName;
    }

    /** Returns the same context and nested mapping. */
    @Override
    public ResolutionResult resolve(SqlQueryContext<?, Q, R> context) {
        // Needed item definition comes in the filter, no need for sub-context, mapping is enough.
        return new ResolutionResult(context, mapping);
    }

    @Override
    public SqaleUpdateContext<S, Q, R> resolve(
            SqaleUpdateContext<?, Q, R> context, ItemPath ignored) {
        return null; // TODO something that simply says to reconstruct ext because it was changed
//        return new NestedContainerUpdateContext<>(context, mapping);
    }
}
