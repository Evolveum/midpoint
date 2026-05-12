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

import com.evolveum.midpoint.smart.impl.mappings.ValuesPairSample;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TrimUpperCaseAndStripDiacriticsHeuristicTest extends HeuristicsRuleTest {

    public TrimUpperCaseAndStripDiacriticsHeuristicTest() throws SchemaException, IOException, SAXException {
    }

    @Test
    void inputContainsWhitespaceLowercaseAndDiacritics_heuristicsShouldBeApplicable() {
        final TrimUpperCaseAndStripDiacriticsHeuristic heuristic = new TrimUpperCaseAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("  josé  ", "JOSE");

        assertTrue(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputDoesNotContainWhitespaceLowercaseAndDiacritics_heuristicsShouldNotBeApplicable() {
        final TrimUpperCaseAndStripDiacriticsHeuristic heuristic = new TrimUpperCaseAndStripDiacriticsHeuristic();
        final ValuesPairSample<String, String> valuesPairSample = inboundValuesPair("JOSE", "JOSE");

        assertFalse(heuristic.isApplicable(valuesPairSample));
    }

    @Test
    void inputContainsWhitespaceLowercaseAndDiacritics_inboundScriptIsEvaluated_outputShouldBeWithoutThem()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final TrimUpperCaseAndStripDiacriticsHeuristic heuristic = new TrimUpperCaseAndStripDiacriticsHeuristic();

        final ExpressionType expression = heuristic.inboundExpression(HeuristicsRuleTest::createScriptExpression);
        final String output = this.evaluateExpression(expression, "input", "  josé Müller!  ");

        assertEquals(output, "JOSE MULLER!");
    }

    @Test
    void propertyContainsWhitespaceLowercaseAndDiacritics_outboundScriptIsEvaluated_outputShouldBeWithoutThem()
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final String focusProperty = "name";
        final TrimUpperCaseAndStripDiacriticsHeuristic heuristic = new TrimUpperCaseAndStripDiacriticsHeuristic();

        final ExpressionType expression = heuristic.outboundExpression(focusProperty, HeuristicsRuleTest::createScriptExpression);
        final String output = this.evaluateExpression(expression, focusProperty, "  josé Müller!  ");

        assertEquals(output, "JOSE MULLER!");
    }

}