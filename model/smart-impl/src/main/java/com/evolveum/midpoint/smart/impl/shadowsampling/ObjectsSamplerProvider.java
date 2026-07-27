/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.shadowsampling;

import org.springframework.stereotype.Component;

/**
 * Provider for selecting the appropriate objects sampler based on the operation type.
 * Encapsulates the logic for choosing between different sampling strategies.
 */
@Component
public class ObjectsSamplerProvider {

    private final CorrelationObjectsSampler correlationSampler;
    private final MappingObjectsSampler mappingSampler;

    public ObjectsSamplerProvider(
            CorrelationObjectsSampler correlationSampler,
            MappingObjectsSampler mappingSampler) {
        this.correlationSampler = correlationSampler;
        this.mappingSampler = mappingSampler;
    }

    /**
     * Returns sampler optimized for correlation operations.
     */
    public CorrelationObjectsSampler getCorrelationSampler() {
        return correlationSampler;
    }

    /**
     * Returns sampler optimized for mapping suggestion operations.
     */
    public MappingObjectsSampler getMappingSampler() {
        return mappingSampler;
    }
}
