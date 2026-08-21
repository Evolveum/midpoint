/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.shadowsampling;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmartIntegrationShadowSamplingConfigurationType;

/**
 * Provider for choosing a suitable shadow sampling strategy for the given operation
 * in case of enabled or disabled shadow caching.
 */
@Component
public class ObjectsSamplerProvider {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectsSamplerProvider.class);

    private final ModelService modelService;
    private final SystemObjectCache systemObjectCache;

    public ObjectsSamplerProvider(ModelService modelService, @Nullable SystemObjectCache systemObjectCache) {
        this.modelService = modelService;
        this.systemObjectCache = systemObjectCache;
    }

    /**
     * Returns a sampler bound to the given resource and type definition for correlation operations,
     * checking if specifically the required attribute paths are effectively cached.
     */
    public ObjectsSampler<List<PrismObject<ShadowType>>> getCorrelationSampler(
            ResourceObjectDefinition typeDefinition,
            ResourceType resource,
            @Nullable Collection<ItemPath> requiredAttributePaths) {
        Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
        Objects.requireNonNull(resource, "resource cannot be null");
        boolean cached = requiredAttributePaths != null
                ? areRequiredAttributesCached(typeDefinition, requiredAttributePaths)
                : areAllAttributesCached(typeDefinition);
        return cached
                ? new CorrelationObjectsSamplerWhenShadowCacheEnabled(modelService, resource, typeDefinition, getCorrelationSampleSizeCached())
                : new CorrelationObjectsSamplerWhenShadowCacheDisabled(modelService, resource, typeDefinition, getCorrelationSampleSizeUncached());
    }

    /**
     * Returns a sampler bound to the given resource and type definition for mapping suggestion operations.
     */
    public ObjectsSampler<MappingSampleResult> getMappingSampler(
            ResourceObjectDefinition typeDefinition, ResourceType resource) {
        Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
        Objects.requireNonNull(resource, "resource cannot be null");
        return areAllAttributesCached(typeDefinition)
                ? new MappingObjectsSamplerWhenShadowCacheEnabled(modelService, resource, typeDefinition, getMappingLlmSampleSizeCached(), getMappingValidationSampleSizeCached())
                : new MappingObjectsSamplerWhenShadowCacheDisabled(modelService, resource, typeDefinition, getMappingLlmSampleSizeUncached(), getMappingValidationSampleSizeUncached());
    }

    /**
     * Returns expected sample size for mapping operations based on caching configuration.
     */
    public int getExpectedMappingSampleSize(ResourceObjectDefinition typeDefinition) {
        Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
        return areAllAttributesCached(typeDefinition)
                ? (getMappingLlmSampleSizeCached() + getMappingValidationSampleSizeCached())
                : (getMappingLlmSampleSizeUncached() + getMappingValidationSampleSizeUncached());
    }

    private int getCorrelationSampleSizeCached() {
        var config = getShadowSamplingConfiguration();
        return (config != null && config.getCorrelationSampleSizeCached() != null)
                ? config.getCorrelationSampleSizeCached()
                : CorrelationObjectsSamplerWhenShadowCacheEnabled.DEFAULT_SAMPLE_SIZE;
    }

    private int getCorrelationSampleSizeUncached() {
        var config = getShadowSamplingConfiguration();
        return (config != null && config.getCorrelationSampleSizeUncached() != null)
                ? config.getCorrelationSampleSizeUncached()
                : CorrelationObjectsSamplerWhenShadowCacheDisabled.DEFAULT_SAMPLE_SIZE;
    }

    private int getMappingLlmSampleSizeCached() {
        var config = getShadowSamplingConfiguration();
        return (config != null && config.getMappingLlmSampleSizeCached() != null)
                ? config.getMappingLlmSampleSizeCached()
                : MappingObjectsSamplerWhenShadowCacheEnabled.DEFAULT_LLM_SAMPLE_SIZE;
    }

    private int getMappingValidationSampleSizeCached() {
        var config = getShadowSamplingConfiguration();
        return (config != null && config.getMappingValidationSampleSizeCached() != null)
                ? config.getMappingValidationSampleSizeCached()
                : MappingObjectsSamplerWhenShadowCacheEnabled.DEFAULT_VALIDATION_SAMPLE_SIZE;
    }

    private int getMappingLlmSampleSizeUncached() {
        var config = getShadowSamplingConfiguration();
        return (config != null && config.getMappingLlmSampleSizeUncached() != null)
                ? config.getMappingLlmSampleSizeUncached()
                : MappingObjectsSamplerWhenShadowCacheDisabled.DEFAULT_LLM_SAMPLE_SIZE;
    }

    private int getMappingValidationSampleSizeUncached() {
        var config = getShadowSamplingConfiguration();
        return (config != null && config.getMappingValidationSampleSizeUncached() != null)
                ? config.getMappingValidationSampleSizeUncached()
                : MappingObjectsSamplerWhenShadowCacheDisabled.DEFAULT_VALIDATION_SAMPLE_SIZE;
    }

    @Nullable
    private SmartIntegrationShadowSamplingConfigurationType getShadowSamplingConfiguration() {
        if (systemObjectCache == null) {
            return null;
        }
        try {
            var systemConfig = systemObjectCache.getSystemConfigurationBean(null);
            return SystemConfigurationTypeUtil.getSmartIntegrationShadowSamplingConfiguration(systemConfig);
        } catch (SchemaException e) {
            LOGGER.warn("Failed to get system configuration for shadow sampling: {}", e.getMessage(), e);
            return null;
        }
    }

    private boolean areRequiredAttributesCached(ResourceObjectDefinition typeDefinition, Collection<ItemPath> requiredPaths) {
        return typeDefinition.isCachingEnabled()
                && requiredPaths.stream().allMatch(typeDefinition::isEffectivelyCached);
    }

    private boolean areAllAttributesCached(ResourceObjectDefinition typeDefinition) {
        return typeDefinition.isCachingEnabled()
                && typeDefinition.getAttributeDefinitions().stream()
                        .allMatch(attrDef -> attrDef.isEffectivelyCached(typeDefinition));
    }
}
