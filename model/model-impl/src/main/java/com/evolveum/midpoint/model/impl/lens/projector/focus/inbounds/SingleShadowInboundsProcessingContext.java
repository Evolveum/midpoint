/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContext;
import com.evolveum.midpoint.model.impl.correlation.CorrelationServiceImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.InboundMappingContextSpecification;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectInboundProcessingDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Everything necessary to carry out limited, single-shadow inbounds evaluation.
 *
 * It is to {@link SingleShadowInboundsProcessing} just like {@link LensContext} is to {@link FullInboundsProcessing}.
 *
 * It exists in more flavors depending on the situation: synchronization-time inbounds, or "extra" inbounds
 * e.g. invoked via {@link MidpointFunctions} or {@link CorrelationServiceImpl} method(s).
 *
 * @param <T> the target object type
 */
public interface SingleShadowInboundsProcessingContext<T extends Containerable>
        extends ResourceObjectProcessingContext, DebugDumpable {

    @NotNull T getPreFocus();

    default @NotNull PrismContainerValue<T> getPreFocusAsPcv() {
        //noinspection unchecked
        return getPreFocus().asPrismContainerValue();
    }

    /** Background information for value provenance metadata for inbound mappings related to this shadow. */
    @NotNull InboundMappingContextSpecification getMappingContextSpecification();

    /**
     * Relates to {@link ResourceObjectProcessingContext#getShadowLikeValue()}.
     */
    @NotNull ResourceObjectDefinition getObjectDefinitionRequired() throws SchemaException, ConfigurationException;

    @NotNull ResourceObjectInboundProcessingDefinition getInboundProcessingDefinition()
            throws SchemaException, ConfigurationException;

    /** Returns the archetype OID bound to the object type. Archetypes determined from the focus itself are not returned here. */
    @Nullable String getArchetypeOid();

    // FIXME
    default boolean isBeforeCorrelation() {
        return true;
    }
}
