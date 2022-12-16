/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.correlation.CorrelationServiceImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Everything necessary to carry out pre-clockwork inbounds evaluation.
 *
 * It is to {@link PreInboundsProcessing} just like {@link LensContext} is to {@link ClockworkInboundsProcessing}.
 *
 * It exists in more flavors depending on the situation: synchronization-time inbounds, or "extra" inbounds
 * e.g. invoked via {@link MidpointFunctions} or {@link CorrelationServiceImpl} method(s).
 *
 * @param <F> the focus type
 */
public interface PreInboundsContext<F extends FocusType>
        extends ResourceObjectProcessingContext, DebugDumpable {

    @NotNull F getPreFocus();

    default @NotNull PrismObject<F> getPreFocusAsPrismObject() {
        //noinspection unchecked
        return (PrismObject<F>) getPreFocus().asPrismObject();
    }

    @Nullable ObjectDelta<ShadowType> getResourceObjectDelta();

    @NotNull ResourceObjectDefinition getObjectDefinitionRequired() throws SchemaException, ConfigurationException;

    /** Returns the archetype OID bound to the object type. Archetypes determined from the focus itself are not returned here. */
    @Nullable String getArchetypeOid();
}
