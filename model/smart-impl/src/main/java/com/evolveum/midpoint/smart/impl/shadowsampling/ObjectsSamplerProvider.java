/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.shadowsampling;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Provider for choosing a suitable shadow sampling strategy for the given operation
 * in case of enabled or disabled shadow caching.
 */
@Component
public class ObjectsSamplerProvider {

    private final ModelService modelService;

    public ObjectsSamplerProvider(ModelService modelService) {
        this.modelService = modelService;
    }

    /**
     * Returns a sampler bound to the given resource and type definition for correlation operations.
     */
    public ObjectsSampler<List<PrismObject<ShadowType>>> getCorrelationSampler(
            ResourceObjectDefinition typeDefinition, ResourceType resource) {
        Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
        Objects.requireNonNull(resource, "resource cannot be null");
        return areAllAttributesCached(typeDefinition)
                ? new CorrelationObjectsSamplerWhenShadowCacheEnabled(modelService, resource, typeDefinition)
                : new CorrelationObjectsSamplerWhenShadowCacheDisabled(modelService, resource, typeDefinition);
    }

    /**
     * Returns a sampler bound to the given resource and type definition for mapping suggestion operations.
     */
    public ObjectsSampler<MappingSampleResult> getMappingSampler(
            ResourceObjectDefinition typeDefinition, ResourceType resource) {
        Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
        Objects.requireNonNull(resource, "resource cannot be null");
        return areAllAttributesCached(typeDefinition)
                ? new MappingObjectsSamplerWhenShadowCacheEnabled(modelService, resource, typeDefinition)
                : new MappingObjectsSamplerWhenShadowCacheDisabled(modelService, resource, typeDefinition);
    }

    /**
     * Returns expected sample size for mapping operations based on caching configuration.
     */
    public int getExpectedMappingSampleSize(ResourceObjectDefinition typeDefinition) {
        Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
        return areAllAttributesCached(typeDefinition)
                ? MappingObjectsSamplerWhenShadowCacheEnabled.getExpectedSampleSize()
                : MappingObjectsSamplerWhenShadowCacheDisabled.getExpectedSampleSize();
    }

    private boolean areAllAttributesCached(ResourceObjectDefinition typeDefinition) {
        if (!typeDefinition.isCachingEnabled()) {
            return false;
        }
        return typeDefinition.getAttributeDefinitions().stream()
                .allMatch(attrDef -> attrDef.isEffectivelyCached(typeDefinition));
    }
}
