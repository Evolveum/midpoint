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
 * Contains shadows with their owners, split into LLM and validation samples.
 */
public record ShadowsWithOwnerSampleResult(
        List<ShadowWithOwner> llmSamples,
        List<ShadowWithOwner> validationSamples) {
}
