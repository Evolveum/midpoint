/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.smart.impl.mappings.MappingScriptTestBase;
import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TrimHeuristicTest extends MappingScriptTestBase {

    public TrimHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsLeadingAndTrailingWhitespace_heuristicsShouldBeApplicable() {
        final TrimHeuristic heuristic = new TrimHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("  hello  ", "hello");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputDoesNotContainLeadingAndTrailingWhitespace_heuristicsShouldNotBeApplicable() {
        final TrimHeuristic heuristic = new TrimHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("hello  ", "hello");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsLeadingAndTrailingWhitespace_inboundScriptIsEvaluated_outputShouldBeTrimmed()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final TrimHeuristic heuristic = new TrimHeuristic();

        final ExpressionType expression = heuristic.inboundExpression(MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, "input", "  hello World!  ");

        assertEquals(output, "hello World!");
    }

    @Test
    void propertyContainsLeadingAndTrailingWhitespace_outboundScriptIsEvaluated_outputShouldBeTrimmed()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final String focusProperty = "name";
        final TrimHeuristic heuristic = new TrimHeuristic();

        final ExpressionType expression = heuristic.outboundExpression(focusProperty, MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, focusProperty, "  hello World!  ");

        assertEquals(output, "hello World!");
    }

}