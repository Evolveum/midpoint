/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.schema.statistics.IterationInformation;
import com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil;
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityIterationInformationType;

/**
 *  Asserter that checks iterative task information.
 */
@SuppressWarnings("WeakerAccess")
public class IterationInformationAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivityIterationInformationType information;

    IterationInformationAsserter(ActivityIterationInformationType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public IterationInformationAsserter<RA> assertTotalCounts(int nonFailure, int failure) {
        assertNonFailureCount(nonFailure);
        assertFailureCount(failure);
        return this;
    }

    public IterationInformationAsserter<RA> assertTotalCounts(int success, int failure, int skip) {
        assertSuccessCount(success);
        assertFailureCount(failure);
        assertSkipCount(skip);
        return this;
    }

    public IterationInformationAsserter<RA> assertNonFailureCount(int success) {
        assertEquals("Wrong value of total 'non-failure' counter", success, getNonFailureCount());
        return this;
    }

    public IterationInformationAsserter<RA> assertSuccessCount(int success) {
        assertEquals("Wrong value of total success counter", success, getSuccessCount());
        return this;
    }

    public IterationInformationAsserter<RA> assertSkipCount(int skip) {
        assertEquals("Wrong value of total skip counter", skip, getSkipCount());
        return this;
    }

    public IterationInformationAsserter<RA> assertSuccessCount(int min, int max) {
        assertBetween(getSuccessCount(), min, max, "Total success counter");
        return this;
    }

    public IterationInformationAsserter<RA> assertFailureCount(int failure) {
        assertEquals("Wrong value of total failure counter", failure, getFailureCount());
        return this;
    }

    public IterationInformationAsserter<RA> assertFailureCount(int min, int max) {
        assertBetween(getFailureCount(), min, max, "Total failure counter");
        return this;
    }

    public IterationInformationAsserter<RA> assertLastFailureObjectName(String expected) {
        assertEquals("Wrong 'last failure' object name", expected, getLastFailedObjectName());
        return this;
    }

    private void assertBetween(int actual, int min, int max, String label) {
        if (actual < min) {
            fail(label + " (" + actual + ") is less than minimum expected (" + min + ")");
        } else if (actual > max) {
            fail(label + " (" + actual + ") is more than maximum expected (" + max + ")");
        }
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public IterationInformationAsserter<RA> display() {
        IntegrationTestTools.display(desc(), IterationInformation.format(information));
        return this;
    }

    // TODO deep or shallow?
    private int getSuccessCount() {
        return TaskOperationStatsUtil.getItemsProcessedWithSuccessShallow(information);
    }

    // TODO deep or shallow?
    private int getFailureCount() {
        return TaskOperationStatsUtil.getItemsProcessedWithFailureShallow(information);
    }

    // TODO deep or shallow?
    private int getSkipCount() {
        return TaskOperationStatsUtil.getItemsProcessedWithSkipShallow(information);
    }

    private int getNonFailureCount() {
        return getSuccessCount() + getSkipCount();
    }

    // TODO deep or shallow?
    private String getLastFailedObjectName() {
        return TaskOperationStatsUtil.getLastProcessedObjectName(information, OutcomeKeyedCounterTypeUtil::isFailure);
    }
}
