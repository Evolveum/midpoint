/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertSame;

import java.util.List;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests conversion of raw expression results to Prism values.
 *
 * Verifies that existing {@link PrismValue} instances are preserved when
 * no output definition is available instead of being wrapped again.
 */
public class TestOutputValuesConvertor extends AbstractUnitTest {

    private PrismContext prismContext;
    private Protector protector;

    @BeforeClass
    public void setUp() throws Exception {
        SchemaDebugUtil.initializePrettyPrinter();
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        prismContext = PrismTestUtil.createInitializedPrismContext();
        protector = ExpressionTestUtil.createInitializedProtector(prismContext);
    }

    @Test
    public void testDirectPrismPropertyValueIsPreserved() throws Exception {
        PrismPropertyValue<String> input = prismContext.itemFactory().createPropertyValue("value");

        assertPreserved(input);
    }

    @Test
    public void testDirectPrismReferenceValueIsPreserved() throws Exception {
        PrismReferenceValue input = prismContext.itemFactory().createReferenceValue("reference-oid");

        assertPreserved(input);
    }

    @Test
    public void testDirectPrismContainerValueIsPreserved() throws Exception {
        PrismContainerValue<AssignmentType> input = new AssignmentType()
                .id(123L)
                .asPrismContainerValue();

        assertPreserved(input);
    }

    private void assertPreserved(PrismValue input) throws Exception {
        List<PrismValue> converted = new OutputValuesConvertor(
                protector, null, null, getTestNameShort())
                .convertResultToPrismValues(input);

        assertEquals(1, converted.size());
        assertSame(input, converted.get(0));
    }
}
