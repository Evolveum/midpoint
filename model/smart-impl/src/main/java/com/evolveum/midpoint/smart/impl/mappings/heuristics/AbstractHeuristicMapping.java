/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * Base class for heuristic mappings that provides helper methods for creating script expressions.
 */
public abstract class AbstractHeuristicMapping implements HeuristicMapping {

    /**
     * Creates a Groovy script expression from the given code and description.
     */
    protected ExpressionType createScriptExpression(String groovyCode, String description) {
        return new ExpressionType()
                .description(description)
                .expressionEvaluator(
                        new ObjectFactory().createScript(
                                new ScriptExpressionEvaluatorType().code(groovyCode)));
    }
}
