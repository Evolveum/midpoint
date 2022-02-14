/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundMappingInContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class PreShadowInboundsPreparation<F extends FocusType> extends ShadowInboundsPreparation<F> {

    PreShadowInboundsPreparation(
            @NotNull PathKeyedMap<List<InboundMappingInContext<?, ?>>> mappingsMap,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result,
            @Nullable PrismObject<F> focus,
            @NotNull PrismObjectDefinition<F> focusDefinition,
            @Nullable PrismObject<ShadowType> currentShadow,
            @Nullable ObjectDelta<ShadowType> aPrioriDelta,
            ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ModelBeans beans) {
        super(
                mappingsMap,
                new PreSource(
                        currentShadow,
                        aPrioriDelta,
                        resourceObjectDefinition),
                new PreTarget<>(focus, focusDefinition),
                new PreContext(env, result, beans));
    }

    @Override
    void evaluateSpecialInbounds() {
        // Not implemented yet
    }
}
