/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Result of attempting to apply a heuristic mapping.
 */
public record HeuristicResult(
        String heuristicName,
        ExpressionType expression,
        float quality) {
}
