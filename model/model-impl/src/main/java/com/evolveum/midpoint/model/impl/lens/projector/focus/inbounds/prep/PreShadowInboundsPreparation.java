/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import java.util.List;

import com.evolveum.midpoint.prism.ItemDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundMappingInContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class PreShadowInboundsPreparation<F extends FocusType> extends ShadowInboundsPreparation<F> {

    public PreShadowInboundsPreparation(
            @NotNull PathKeyedMap<List<InboundMappingInContext<?, ?>>> mappingsMap,
            @NotNull PathKeyedMap<ItemDefinition<?>> itemDefinitionMap,
            @NotNull PreContext context,
            @Nullable PrismObject<F> focus,
            @NotNull PrismObjectDefinition<F> focusDefinition)
            throws SchemaException, ConfigurationException {
        super(
                mappingsMap,
                new PreSource(context.ctx),
                new PreTarget<>(focus, focusDefinition, itemDefinitionMap),
                context);
    }

    @Override
    void evaluateSpecialInbounds() {
        // Not implemented yet
    }
}
