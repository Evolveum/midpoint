/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.objdef;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.impl.BaseItemMerger;
import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.prism.impl.GenericItemMerger;
import com.evolveum.midpoint.prism.OriginMarker;
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
 * Approximate solution, until MID-7929 is resolved:
 *
 * We (independently) merge definitions for each layer (schema, model, presentation), and also definitions
 * that have no layer specified.
 *
 * This should work with any interpretation of the {@link PropertyLimitationsType} in the follow-on code.
 */
public class LimitationsMerger extends BaseItemMerger<PrismContainer<PropertyLimitationsType>> {

    public LimitationsMerger(@Nullable OriginMarker originMarker) {
        super(originMarker);
    }

    @Override
    protected void mergeInternal(
            @NotNull PrismContainer<PropertyLimitationsType> target,
            @NotNull PrismContainer<PropertyLimitationsType> source) throws ConfigurationException, SchemaException {

        Collection<PropertyLimitationsType> targetRealValues = target.getRealValues();
        Collection<PropertyLimitationsType> sourceRealValues = source.getRealValues();

        List<PropertyLimitationsType> merged = new ArrayList<>();
        for (LayerType layer : LayerType.values()) {
            mergeForConfiguredLayer(targetRealValues, sourceRealValues, layer, merged);
        }
        mergeForConfiguredLayer(targetRealValues, sourceRealValues, null, merged);

        target.clear();
        for (PropertyLimitationsType mergedForLayer : merged) {
            //noinspection unchecked
            target.add(mergedForLayer.asPrismContainerValue());
        }
    }

    private void mergeForConfiguredLayer(
            @NotNull Collection<PropertyLimitationsType> targetRealValues,
            @NotNull Collection<PropertyLimitationsType> sourceRealValues,
            @Nullable LayerType configuredLayer,
            @NotNull List<PropertyLimitationsType> merged) throws SchemaException, ConfigurationException {
        PropertyLimitationsType targetLimitations = MiscSchemaUtil.getLimitationsLabeled(targetRealValues, configuredLayer);
        PropertyLimitationsType sourceLimitations = MiscSchemaUtil.getLimitationsLabeled(sourceRealValues, configuredLayer);
        if (targetLimitations == null && sourceLimitations == null) {
            // nothing to put into the result
        } else if (targetLimitations == null) {
            merged.add(
                    restrictToLayer(sourceLimitations, configuredLayer));
        } else if (sourceLimitations == null) {
            merged.add(
                    restrictToLayer(targetLimitations, configuredLayer));
        } else {
            merged.add(
                    restrictToLayer(
                            mergeLimitations(targetLimitations, sourceLimitations),
                            configuredLayer));
        }
    }

    private PropertyLimitationsType mergeLimitations(
            @NotNull PropertyLimitationsType targetLimitations,
            @NotNull PropertyLimitationsType sourceLimitations) throws SchemaException, ConfigurationException {
        new BaseMergeOperation<>(
                targetLimitations,
                sourceLimitations,
                new GenericItemMerger(originMarker, new PathKeyedMap<>()))
                .execute();
        return targetLimitations;
    }

    /**
     * Takes limitations bean (compatible with the specified layer), and if it's targeted to multiple layers,
     * keeps only the specified layer.
     */
    private @NotNull PropertyLimitationsType restrictToLayer(
            @NotNull PropertyLimitationsType limitations,
            @Nullable LayerType layer) {
        List<LayerType> existingLayerList = limitations.getLayer();
        if (layer == null) {
            assert existingLayerList.isEmpty(); // Ensured by MiscSchemaUtil.getLimitationsLabeled
            return limitations;
        } else {
            if (existingLayerList.size() == 1) {
                assert existingLayerList.get(0) == layer; // Ensured by MiscSchemaUtil.getLimitationsLabeled
                return limitations;
            } else {
                PropertyLimitationsType clone = limitations.clone();
                clone.getLayer().clear();
                clone.getLayer().add(layer);
                return clone;
            }
        }
    }
}
