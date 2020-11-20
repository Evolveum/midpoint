/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultImportanceType.MAJOR;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultHandlingStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public class TestOperationResult extends AbstractSchemaTest {

    private static final String LOCAL_1 = "local1";

    @Test
    public void testCleanup() throws Exception {
        given("checks also conversions during result construction");
        OperationResult root = new OperationResult("dummy");
        checkResultConversion(root, true);

        OperationResult sub1 = root.createSubresult("sub1");
        checkResultConversion(root, true);

        OperationResult sub11 = sub1.createMinorSubresult("sub11");
        OperationResult sub12 = sub1.createMinorSubresult("sub12");
        OperationResult sub13 = sub1.createSubresult("sub13");

        OperationResult sub2 = root.createSubresult("sub2");
        sub2.recordFatalError("Fatal");
        checkResultConversion(root, true);

        sub11.recordSuccess();
        sub12.recordWarning("Warning");
        sub13.recordSuccess();
        checkResultConversion(root, true);

        when();
        System.out.println("Before cleanup:\n" + root.debugDump());
        sub1.computeStatus();
        sub1.cleanupResult();
        root.computeStatus();
        root.cleanupResult();

        then();
        System.out.println("After cleanup (normal):\n" + root.debugDump());
        assertEquals("Wrong overall status", OperationResultStatus.FATAL_ERROR, root.getStatus()); // because of sub2
        assertEquals("Wrong status of sub1", OperationResultStatus.WARNING, sub1.getStatus()); // because of sub12
        assertEquals("Wrong # of sub1 subresults", 2, sub1.getSubresults().size());

        checkResultConversion(root, true);

        when();
        PrismContext prismContext = getPrismContext();
        OperationResult.applyOperationResultHandlingStrategy(
                Arrays.asList(
                        new OperationResultHandlingStrategyType(prismContext)
                                .global(true),
                        new OperationResultHandlingStrategyType(prismContext)
                                .name(LOCAL_1)
                                .preserveDuringCleanup(MAJOR)
                ), null);
        OperationResult.setThreadLocalHandlingStrategy(LOCAL_1);

        root.cleanupResultDeeply();
        System.out.println("After deep cleanup (major):\n" + root.debugDump());

        then();
        assertEquals("Wrong overall status", OperationResultStatus.FATAL_ERROR, root.getStatus()); // because of sub2
        assertEquals("Wrong status of sub1", OperationResultStatus.WARNING, sub1.getStatus()); // because of sub12
        assertEquals("Wrong # of sub1 subresults", 1, sub1.getSubresults().size()); // SUCCESS should be gone now
    }

    @Test
    public void testSummarizeByHiding() throws Exception {
        given();
        OperationResult root = new OperationResult("dummy");
        OperationResult level1 = root.createSubresult("level1");
        for (int i = 1; i <= 30; i++) {
            OperationResult level2 = level1.createSubresult("level2");
            level2.addParam("value", i);
            level2.recordSuccess();
        }
        level1.computeStatus();
        root.computeStatus();

        when();
        root.summarize();
        System.out.println("After shallow summarizing\n" + root.debugDump());

        then();
        assertEquals("Level1 shouldn't be summarized this time", 30, level1.getSubresults().size());

        when();
        root.summarize(true);
        System.out.println("After deep summarizing\n" + root.debugDump());

        then();
        assertEquals("Level1 should be summarized this time", 11, level1.getSubresults().size());

        OperationResult summary = level1.getSubresults().get(10);
        assertEquals("Wrong operation in summary", "level2", summary.getOperation());
        assertEquals("Wrong status in summary", OperationResultStatus.SUCCESS, summary.getStatus());
        assertEquals("Wrong hidden records count in summary", 20, summary.getHiddenRecordsCount());

        checkResultConversion(root, true);
    }

    @Test
    public void testExplicitSummarization() throws Exception {
        given();
        OperationResult root = new OperationResult("dummy");
        OperationResult level1 = root.createSubresult("level1");
        level1.setSummarizeSuccesses(true);
        for (int i = 1; i <= 30; i++) {
            OperationResult level2 = level1.createSubresult("level2");
            level2.addParam("value", i);
            level2.recordStatus(OperationResultStatus.SUCCESS, "message");
        }
        level1.computeStatus();
        root.computeStatus();

        when();
        root.summarize();
        System.out.println("After shallow summarizing\n" + root.debugDump());

        then();
        assertEquals("Level1 shouldn't be summarized this time", 30, level1.getSubresults().size());

        when();
        root.summarize(true);
        System.out.println("After deep summarizing\n" + root.debugDump());

        then();
        assertEquals("Level1 should be summarized this time", 1, level1.getSubresults().size());

        OperationResult summary = level1.getSubresults().get(0);
        assertEquals("Wrong operation in summary", "level2", summary.getOperation());
        assertEquals("Wrong status in summary", OperationResultStatus.SUCCESS, summary.getStatus());
        assertEquals("Wrong message in summary", "message", summary.getMessage());
        assertEquals("Wrong count in summary", 30, summary.getCount());

        checkResultConversion(root, false); // summarization settings are not serialized
    }

    @Test
    public void testIncrementalSummarization() throws Exception {
        OperationResult root = new OperationResult("dummy");
        int b = 0;
        for (int a = 1; a <= 30; a++) {
            OperationResult operationA = root.createSubresult("operationA");
            operationA.addParam("valueA", a);
            operationA.recordStatus(OperationResultStatus.SUCCESS, "messageA");

            if (a % 2 == 1) {
                OperationResult operationB = root.createSubresult("operationB");
                operationB.addParam("valueB", ++b);
                operationB.recordStatus(OperationResultStatus.WARNING, "messageB");
            }

            OperationResult operationC = root.createSubresult("operationC." + a); // will not be summarized
            operationC.addParam("valueC", a);
            operationC.recordStatus(OperationResultStatus.SUCCESS, "messageC");

            root.summarize();
            System.out.println("After iteration " + a + ":\n" + root.debugDump());
            int expectedA = Math.min(a, 10);
            int expectedB = Math.min(b, 10);
            int expectedSummarizationA = a <= 10 ? 0 : 1;
            int expectedSummarizationB = b <= 10 ? 0 : 1;
            int expectedTotal = expectedA + expectedB + a + expectedSummarizationA + expectedSummarizationB;

            if (b > 10) {
                assertEquals("Wrong # of subresults", expectedTotal, root.getSubresults().size());
                List<OperationResult> lastTwo = root.getSubresults().subList(expectedTotal - 2, expectedTotal);
                OperationResult sumA, sumB;
                if ("operationA".equals(lastTwo.get(0).getOperation())) {
                    sumA = lastTwo.get(0);
                    sumB = lastTwo.get(1);
                } else {
                    sumA = lastTwo.get(1);
                    sumB = lastTwo.get(0);
                }
                assertEquals("Wrong operation in summary for A", "operationA", sumA.getOperation());
                assertEquals("Wrong operation in summary for B", "operationB", sumB.getOperation());
                assertEquals("Wrong status in summary for A", OperationResultStatus.SUCCESS, sumA.getStatus());
                assertEquals("Wrong status in summary for B", OperationResultStatus.WARNING, sumB.getStatus());
                assertEquals("Wrong hidden records count in summary for A", a - expectedA, sumA.getHiddenRecordsCount());
                assertEquals("Wrong hidden records count in summary for B", b - expectedB, sumB.getHiddenRecordsCount());
            }
        }

        checkResultConversion(root, true);
    }

    private void checkResultConversion(OperationResult result, boolean assertEquals) throws SchemaException {
        when();
        OperationResultType resultType = result.createOperationResultType();
        String serialized = getPrismContext().xmlSerializer().serializeAnyData(resultType, SchemaConstants.C_RESULT);
        System.out.println("Converted OperationResultType\n" + serialized);
        OperationResult resultRoundTrip = OperationResult.createOperationResult(resultType);
        OperationResultType resultTypeRoundTrip = resultRoundTrip.createOperationResultType();

        then();
        assertEquals("Operation result conversion changes the result (OperationResultType)", resultType, resultTypeRoundTrip);
        if (assertEquals) {
            assertEquals("Operation result conversion changes the result (OperationResult)", result, resultRoundTrip);
        }
    }
}
