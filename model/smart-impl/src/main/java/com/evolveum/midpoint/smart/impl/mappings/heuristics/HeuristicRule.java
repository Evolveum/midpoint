/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Represents a simple transformation rule that can be applied to attribute mappings.
 * Rules are combined algorithmically by the heuristic manager to find the best fit.
 *
 * Rules are simple, fast transformations like toLowerCase(), trim(), etc.
 * that can be validated against sample data to determine if they produce correct mappings.
 */
public interface HeuristicRule {

    /**
     * Returns a unique name for this rule (e.g., "toLowerCase", "trim").
     */
    String getName();

    /**
     * Returns a human-readable description of what this rule does.
     */
    String getDescription();

    /**
     * Checks if this rule is potentially applicable to the given sample data.
     * This is a quick pre-filter to avoid expensive quality assessment for obviously incompatible rules.
     */
    boolean isApplicable(ValuesPairSample<?, ?> sample);

    /**
     * Creates the expression for inbound mapping using the provided factory.
     * Uses 'input' as the variable name for the source shadow attribute.
     * Returns null if this rule represents "asIs" mapping (no transformation).
     *
     * @param factory the factory to create the expression
     * @return the expression or null for no transformation
     */
    ExpressionType inboundExpression(MappingExpressionFactory factory);

    /**
     * Creates the expression for outbound mapping using the provided factory.
     * Uses the actual focus property name as the variable (e.g., 'personalNumber', 'givenName').
     * Returns null if this rule represents "asIs" mapping (no transformation).
     *
     * @param focusPropertyName the name of the focus property
     * @param factory the factory to create the expression
     * @return the expression or null for no transformation
     */
    ExpressionType outboundExpression(String focusPropertyName, MappingExpressionFactory factory);
}
