/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings.heuristics;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

public class TrimLowerCaseAndStripDiacriticsHeuristicTest extends MappingScriptTestBase {

    public TrimLowerCaseAndStripDiacriticsHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsWhitespaceUppercaseAndDiacritics_heuristicsShouldBeApplicable() {
        final TrimLowerCaseAndStripDiacriticsHeuristic heuristic = new TrimLowerCaseAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("  JOSÉ  ", "jose");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputDoesNotContainWhitespaceUppercaseNorDiacritics_heuristicsShouldNotBeApplicable() {
        final TrimLowerCaseAndStripDiacriticsHeuristic heuristic = new TrimLowerCaseAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("jose", "jose");

        assertFalse(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsWhitespaceUppercaseAndDiacritics_inboundScriptIsEvaluated_outputShouldBeTrimmedLowercaseAndWithoutDiacritics()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final TrimLowerCaseAndStripDiacriticsHeuristic heuristic = new TrimLowerCaseAndStripDiacriticsHeuristic();

        final ExpressionType expression = heuristic.inboundExpression(MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, "input", "  JOSÉ Müller!  ");

        assertEquals(output, "jose muller!");
    }

    @Test
    void propertyContainsWhitespaceUppercaseAndDiacritics_outboundScriptIsEvaluated_outputShouldBeTrimmedLowercaseAndWithoutDiacritics()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final String focusProperty = "name";
        final TrimLowerCaseAndStripDiacriticsHeuristic heuristic = new TrimLowerCaseAndStripDiacriticsHeuristic();

        final ExpressionType expression = heuristic.outboundExpression(focusProperty, MappingScriptTestBase::createScriptExpression);
        final String output = this.evaluateExpression(expression, focusProperty, "  JOSÉ Müller!  ");

        assertEquals(output, "jose muller!");
    }

}