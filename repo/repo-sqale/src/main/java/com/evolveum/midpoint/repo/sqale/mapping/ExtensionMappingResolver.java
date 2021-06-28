/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.update.ExtensionUpdateContext;
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
    private final Function<Q, JsonbPath> rootToExtensionPath;

    public ExtensionMappingResolver(
            @NotNull ExtensionMapping<S, Q, R> mapping,
            @NotNull Function<Q, JsonbPath> rootToExtensionPath) {
        this.mapping = mapping;
        this.rootToExtensionPath = rootToExtensionPath;
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
        JsonbPath jsonbPath = rootToExtensionPath.apply(context.entityPath());
        return new ExtensionUpdateContext<>(context, mapping, jsonbPath);
    }
}
