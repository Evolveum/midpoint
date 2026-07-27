/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.shadowsampling;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

/**
 * Sampling configuration optimized for mapping suggestion operations.
 * Splits samples into two groups: smaller set for LLM analysis and larger set for validation.
 * Uses larger sample sizes when caching is enabled.
 */
public class SamplingConfigurationForMappings extends SamplingConfiguration {
    /** Default LLM sample size for non-cached resources - smaller for cost efficiency */
    private static final int DEFAULT_LLM_SAMPLE_SIZE = 20;
    /** Default validation sample size for non-cached resources */
    private static final int DEFAULT_VALIDATION_SAMPLE_SIZE = 200;
    /** Larger LLM sample size when caching is enabled */
    private static final int CACHED_LLM_SAMPLE_SIZE = 50;
    /** Larger validation sample size when caching is enabled */
    private static final int CACHED_VALIDATION_SAMPLE_SIZE = 1000;

    private final int llmSampleSize;
    private final int validationSampleSize;

    private SamplingConfigurationForMappings(int totalSampleSize, int llmSampleSize, int validationSampleSize,
            boolean useNoFetch, boolean useReadOnly) {
        super(totalSampleSize, useNoFetch, useReadOnly);
        this.llmSampleSize = llmSampleSize;
        this.validationSampleSize = validationSampleSize;
    }

    public static SamplingConfigurationForMappings create(ResourceObjectDefinition typeDefinition) {
        boolean useNoFetch = typeDefinition.isCachingEnabled();
        boolean useReadOnly = true;
        int llmSize = useNoFetch ? CACHED_LLM_SAMPLE_SIZE : DEFAULT_LLM_SAMPLE_SIZE;
        int validationSize = useNoFetch ? CACHED_VALIDATION_SAMPLE_SIZE : DEFAULT_VALIDATION_SAMPLE_SIZE;
        int sampleSize = llmSize + validationSize;

        return new SamplingConfigurationForMappings(sampleSize, llmSize, validationSize, useNoFetch, useReadOnly);
    }

    public int getLlmSampleSize() {
        return llmSampleSize;
    }

    public int getValidationSampleSize() {
        return validationSampleSize;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "llmSampleSize=" + llmSampleSize +
                ", validationSampleSize=" + validationSampleSize +
                ", totalSampleSize=" + getSampleSize() +
                ", useNoFetch=" + useNoFetch +
                ", useReadOnly=" + useReadOnly +
                '}';
    }
}
