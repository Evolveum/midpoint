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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Provider for selecting the appropriate objects sampler based on the operation type and caching configuration.
 * Encapsulates the logic for choosing between cached and uncached sampling strategies.
 */
@Component
public class ObjectsSamplerProvider {

    private final CachedCorrelationObjectsSampler cachedCorrelationSampler;
    private final UncachedCorrelationObjectsSampler uncachedCorrelationSampler;
    private final CachedMappingObjectsSampler cachedMappingSampler;
    private final UncachedMappingObjectsSampler uncachedMappingSampler;

    public ObjectsSamplerProvider(
            CachedCorrelationObjectsSampler cachedCorrelationSampler,
            UncachedCorrelationObjectsSampler uncachedCorrelationSampler,
            CachedMappingObjectsSampler cachedMappingSampler,
            UncachedMappingObjectsSampler uncachedMappingSampler) {
        this.cachedCorrelationSampler = cachedCorrelationSampler;
        this.uncachedCorrelationSampler = uncachedCorrelationSampler;
        this.cachedMappingSampler = cachedMappingSampler;
        this.uncachedMappingSampler = uncachedMappingSampler;
    }

    /**
     * Returns sampler optimized for correlation operations based on caching configuration.
     */
    public ObjectsSampler<List<PrismObject<ShadowType>>> getCorrelationSampler(ResourceObjectDefinition typeDefinition) {
        Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
        return typeDefinition.isCachingEnabled()
                ? cachedCorrelationSampler
                : uncachedCorrelationSampler;
    }

    /**
     * Returns sampler optimized for mapping suggestion operations based on caching configuration.
     */
    public ObjectsSampler<MappingSampleResult> getMappingSampler(ResourceObjectDefinition typeDefinition) {
        Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
        return typeDefinition.isCachingEnabled()
                ? cachedMappingSampler
                : uncachedMappingSampler;
    }

    /**
     * Returns expected sample size for mapping operations based on caching configuration.
     */
    public int getExpectedMappingSampleSize(ResourceObjectDefinition typeDefinition) {
        Objects.requireNonNull(typeDefinition, "typeDefinition cannot be null");
        return typeDefinition.isCachingEnabled()
                ? cachedMappingSampler.getExpectedSampleSize()
                : uncachedMappingSampler.getExpectedSampleSize();
    }
}
