
package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Represents a general-purpose transformation heuristic that can be applied
 * to attribute mappings before falling back to expensive LLM calls.
 *
 * Heuristics are simple, fast transformations like toLowerCase(), trim(), etc.
 * that can be validated against sample data to determine if they produce correct mappings.
 */
public interface HeuristicMapping {

    /**
     * Returns a unique name for this heuristic (e.g., "toLowerCase", "trim").
     */
    String getName();

    /**
     * Returns a human-readable description of what this heuristic does.
     */
    String getDescription();

    /**
     * Checks if this heuristic is potentially applicable to the given sample data.
     * This is a quick pre-filter to avoid expensive quality assessment for obviously incompatible heuristics.
     */
    boolean isApplicable(ValuesPairSample<?, ?> sample);

    /**
     * Generates the expression (typically a Groovy script) for inbound mapping.
     * Uses 'input' as the variable name for the source shadow attribute.
     * Returns null if this heuristic represents "asIs" mapping (no transformation).
     */
    ExpressionType generateInboundExpression();

    /**
     * Generates the expression (typically a Groovy script) for outbound mapping.
     * Uses the actual focus property name as the variable (e.g., 'personalNumber', 'givenName').
     * Returns null if this heuristic represents "asIs" mapping (no transformation).
     */
    ExpressionType generateOutboundExpression(String focusPropertyName);
}
