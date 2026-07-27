/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.mappings;

import java.util.List;

/**
 * Result of shadow sampling operation that includes owners.
 * Contains shadows with their owners, along with information about
 * how to split them into LLM and validation samples.
 */
public record ShadowsWithOwnerSampleResult(
        List<ShadowWithOwner> samples,
        int llmSampleSize,
        int validationSampleSize) {

    public int getTotalSampleSize() {
        return samples.size();
    }
}
