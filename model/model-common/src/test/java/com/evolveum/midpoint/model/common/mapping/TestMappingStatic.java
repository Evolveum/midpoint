/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * @author Radovan Semancik
 */
public class TestMappingStatic extends AbstractUnitTest {

    private MappingTestEvaluator evaluator;

    @BeforeClass
    public void setupFactory() throws SAXException, IOException, SchemaException {
        evaluator = new MappingTestEvaluator();
        evaluator.init();
    }

    @Test
    public void testValueSingleDeep() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
                "mapping-value-single-deep.xml", getTestNameShort(), "costCenter");

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "foobar");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueSingleShallow() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
                "mapping-value-single-shallow.xml", getTestNameShort(), "costCenter");

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "foobar");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueMultiDeep() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
                "mapping-value-multi-deep.xml", getTestNameShort(), "subtype");

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testValueMultiShallow() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
                "mapping-value-multi-shallow.xml", getTestNameShort(), "subtype");

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "12345", "67890");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testPathNoSource() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
                "mapping-path-system-variables-nosource.xml", getTestNameShort(), "subtype");

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "jack");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }

    @Test
    public void testConstFoo() throws Exception {
        // WHEN
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = evaluator.evaluateMapping(
                "mapping-const-foo.xml", getTestNameShort(), "costCenter");

        // THEN
        PrismAsserts.assertTripleZero(outputTriple, "foobar");
        PrismAsserts.assertTripleNoPlus(outputTriple);
        PrismAsserts.assertTripleNoMinus(outputTriple);
    }
}
