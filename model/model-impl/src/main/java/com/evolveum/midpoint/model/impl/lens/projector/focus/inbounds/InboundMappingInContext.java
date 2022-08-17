/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Inbound mapping along with its {@link LensProjectionContext} (if relevant).
 *
 * @param <V> type of mapping output value
 * @param <D> type of mapping output value definition (property, container, ...)
 */
public class InboundMappingInContext<V extends PrismValue, D extends ItemDefinition<?>> {

    @NotNull private final MappingImpl<V, D> mapping;

    /**
     * Note that the projection context is non-null if the mapping is evaluated as part of the clockwork execution.
     * (Null if in pre-inbounds evaluation.)
     */
    @Nullable private final LensProjectionContext projectionContext;

    public InboundMappingInContext(@NotNull MappingImpl<V, D> mapping, @Nullable LensProjectionContext projectionContext) {
        this.mapping = mapping;
        this.projectionContext = projectionContext;
    }

    public @NotNull MappingImpl<V, D> getMapping() {
        return mapping;
    }

    public @Nullable LensProjectionContext getProjectionContext() {
        return projectionContext;
    }

    @NotNull LensProjectionContext getProjectionContextRequired() {
        return Objects.requireNonNull(projectionContext);
    }

    @NotNull ProjectionContextKey getProjectionContextKeyRequired() {
        return getProjectionContextRequired().getKey();
    }

    public @Nullable LensContext<?> getLensContext() {
        return projectionContext != null ? projectionContext.getLensContext() : null;
    }

    boolean isProjectionBeingDeleted() {
        return projectionContext != null && projectionContext.isDelete();
    }

    boolean isProjectionGone() {
        return projectionContext != null && projectionContext.isGone();
    }

    @Override
    public String toString() {
        return mapping + (projectionContext != null ? " in " + projectionContext.getHumanReadableName() : "");
    }
}
