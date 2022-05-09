/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.objdef;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.merger.BaseCustomItemMerger;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyLimitationsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A merger specific to {@link PropertyLimitationsType}.
 *
 * Unfinished work. We need to know the exact semantics of that type first.
 */
class LimitationsMerger extends BaseCustomItemMerger<PrismProperty<PropertyLimitationsType>> {

    @Override
    protected void mergeInternal(
            @NotNull PrismProperty<PropertyLimitationsType> target,
            @NotNull PrismProperty<PropertyLimitationsType> source) throws ConfigurationException, SchemaException {

        Collection<PropertyLimitationsType> targetRealValues = target.getRealValues();
        Collection<PropertyLimitationsType> sourceRealValues = source.getRealValues();
        List<PropertyLimitationsType> mergedForAllLayers = new ArrayList<>();
        for (LayerType layer : LayerType.values()) {
            mergedForAllLayers.add(
                    createMerged(
                            layer,
                            MiscSchemaUtil.getLimitationsLabeled(targetRealValues, layer),
                            MiscSchemaUtil.getLimitationsLabeled(sourceRealValues, layer)));
        }
        target.clear();
        for (PropertyLimitationsType mergedForLayer : mergedForAllLayers) {
            target.addRealValue(mergedForLayer);
        }
    }

    private PropertyLimitationsType createMerged(
            @NotNull LayerType layer,
            @Nullable PropertyLimitationsType target,
            @Nullable PropertyLimitationsType source) {
        throw new UnsupportedOperationException("Do not even try to merge property limitations before MID-7929 is resolved!");
    }
}
