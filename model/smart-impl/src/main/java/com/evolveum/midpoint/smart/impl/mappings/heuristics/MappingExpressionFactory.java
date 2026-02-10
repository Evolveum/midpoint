/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Factory for creating mapping expressions from Groovy code.
 * This functional interface allows heuristic rules to remain independent
 * of the specific expression creation mechanism.
 */
@FunctionalInterface
interface MappingExpressionFactory {

    /**
     * Creates a script expression from the given Groovy code and description.
     *
     * @param groovyCode the Groovy script code
     * @param description human-readable description of what the expression does
     * @return the expression type containing the script
     */
    ExpressionType createScriptExpression(String groovyCode, String description);
}
