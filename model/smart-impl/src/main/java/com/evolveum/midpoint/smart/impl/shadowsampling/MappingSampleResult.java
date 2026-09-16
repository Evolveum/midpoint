/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.shadowsampling;

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Result object containing shadow samples split for mapping suggestion operations.
 * Shadows are divided into a smaller set for LLM analysis and a larger set for validation.
 */
public record MappingSampleResult(
        List<PrismObject<ShadowType>> llmSamples,
        List<PrismObject<ShadowType>> validationSamples) {
}
