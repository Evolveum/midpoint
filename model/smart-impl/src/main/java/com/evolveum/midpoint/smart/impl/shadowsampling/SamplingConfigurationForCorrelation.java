/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.shadowsampling;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

/**
 * Sampling configuration optimized for correlation operations.
 * Uses larger sample sizes when caching is enabled to improve correlation accuracy.
 */
public class SamplingConfigurationForCorrelation extends SamplingConfiguration {
    /** Default sample size for non-cached resources */
    private static final int DEFAULT_SAMPLE_SIZE = 2000;
    /** Larger sample size when caching is enabled for better accuracy */
    private static final int CACHED_SAMPLE_SIZE = 5000;

    private SamplingConfigurationForCorrelation(int sampleSize, boolean useNoFetch, boolean useReadOnly) {
        super(sampleSize, useNoFetch, useReadOnly);
    }

    public static SamplingConfigurationForCorrelation create(ResourceObjectDefinition typeDefinition) {
        boolean useNoFetch = typeDefinition.isCachingEnabled();
        int sampleSize = useNoFetch ? CACHED_SAMPLE_SIZE : DEFAULT_SAMPLE_SIZE;
        boolean useReadOnly = true;

        return new SamplingConfigurationForCorrelation(sampleSize, useNoFetch, useReadOnly);
    }
}
