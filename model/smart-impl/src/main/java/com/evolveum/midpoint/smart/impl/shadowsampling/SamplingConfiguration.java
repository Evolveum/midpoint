/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.shadowsampling;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

import java.util.Collection;

/**
 * Base configuration for sampling shadows and focus objects.
 * Determines sample size and options based on cache availability and total object count.
 */
public abstract class SamplingConfiguration {
    protected int sampleSize;
    protected boolean useNoFetch;
    protected boolean useReadOnly;

    protected SamplingConfiguration(int sampleSize, boolean useNoFetch, boolean useReadOnly) {
        this.sampleSize = sampleSize;
        this.useNoFetch = useNoFetch;
        this.useReadOnly = useReadOnly;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    /**
     * Creates GetOperationOptions based on this configuration.
     */
    Collection<SelectorOptions<GetOperationOptions>> createGetOptions() {
        if (!useNoFetch && !useReadOnly) {
            return null;
        }

        GetOperationOptions options = new GetOperationOptions();
        if (useNoFetch) {
            options.setNoFetch(true);
        }
        if (useReadOnly) {
            options.setReadOnly(true);
        }

        return SelectorOptions.createCollection(options);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "sampleSize=" + sampleSize +
                ", useNoFetch=" + useNoFetch +
                ", useReadOnly=" + useReadOnly +
                '}';
    }
}
