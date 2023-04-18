/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.simulations;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.simulation.SimulationMetricComputer;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationResultTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.fail;

/**
 * Tests various utility methods related to computing simulation metrics.
 * (At the schema level i.e. doing basic arithmetics on the values.)
 */
public class TestSimulationMetricComputations extends AbstractSchemaTest {

    public static final File TEST_DIR = new File("src/test/resources/simulations");
    private static final File TEST1_BASE = new File(TEST_DIR, "test1-base.xml");
    private static final File TEST1_DELTA = new File(TEST_DIR, "test1-delta.xml");
    private static final File TEST1_EXPECTED_SUM = new File(TEST_DIR, "test1-expected-sum.xml");
    private static final File TEST1_EXPECTED_SUM_COLLAPSED = new File(TEST_DIR, "test1-expected-sum-collapsed.xml");

    /** sum = base + delta */
    @Test
    public void testAdd1() throws SchemaException, IOException {
        PrismContext prismContext = PrismTestUtil.getPrismContext();
        SimulationResultType base = (SimulationResultType) prismContext.parseObject(TEST1_BASE).asObjectable();
        SimulationResultType delta = (SimulationResultType) prismContext.parseObject(TEST1_DELTA).asObjectable();
        SimulationResultType expectedSum = (SimulationResultType) prismContext.parseObject(TEST1_EXPECTED_SUM).asObjectable();

        when("sum = base + delta");
        List<SimulationMetricValuesType> sum = SimulationMetricComputer.add(base.getMetric(), delta.getMetric());

        then("sum is OK");
        displayValue("sum", DebugUtil.debugDump(sum, 1));
        assertSum(sum, expectedSum.getMetric());
        SimulationResultType sumResult = asResult(sum);

        and("value for 712 is OK");
        SimulationMetricValuesType m712 =
                SimulationResultTypeUtil.getMetricValuesBeanByMarkOid(
                        sumResult, "00000000-0000-0000-0000-000000000712");
        assertThat(SimulationMetricValuesTypeUtil.getValue(m712)).as("value for 712").isEqualTo(BigDecimal.valueOf(14));

        and("value for 716 is OK");
        SimulationMetricValuesType m716 =
                SimulationResultTypeUtil.getMetricValuesBeanByMarkOid(
                        sumResult, "00000000-0000-0000-0000-000000000716");
        assertThat(SimulationMetricValuesTypeUtil.getValue(m716)).as("value for 716").isEqualTo(BigDecimal.valueOf(4));

        when("all dimensions are collapsed");
        var collapsedResult = SimulationResultTypeUtil.collapseDimensions(sumResult);

        then();
        SimulationResultType expectedSumCollapsed =
                (SimulationResultType) prismContext.parseObject(TEST1_EXPECTED_SUM_COLLAPSED).asObjectable();
        displayValue("sum (collapsed)", DebugUtil.debugDump(collapsedResult, 1));
        assertSum(collapsedResult.getMetric(), expectedSumCollapsed.getMetric());
    }

    private SimulationResultType asResult(List<SimulationMetricValuesType> values) {
        var result = new SimulationResultType();
        result.getMetric().addAll(
                CloneUtil.cloneCollectionMembers(values));
        return result;
    }

    private void assertSum(List<SimulationMetricValuesType> sum, List<SimulationMetricValuesType> expectedSum) {
        if (!MiscUtil.unorderedCollectionEquals(sum, expectedSum)) {
            fail("sum does not match expected one");
        }
    }
}
