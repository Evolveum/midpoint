/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.schema.util.task.TaskProgressUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartProgressType;

/**
 *  Asserter that checks `TaskPartProgressType` objects.
 */
@SuppressWarnings("WeakerAccess")
public class TaskPartProgressAsserter<RA> extends AbstractAsserter<RA> {

    private final TaskPartProgressType information;

    TaskPartProgressAsserter(TaskPartProgressType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public TaskPartProgressAsserter<RA> assertOpenCounts(int success, int failure, int skip) {
        assertSuccessCount(success, true);
        assertFailureCount(failure, true);
        assertSkipCount(skip, true);
        return this;
    }

    public TaskPartProgressAsserter<RA> assertSuccessCount(int success, boolean open) {
        assertEquals("Wrong value of total success counter", success, getSuccessCount(open));
        return this;
    }

    public TaskPartProgressAsserter<RA> assertSkipCount(int skip, boolean open) {
        assertEquals("Wrong value of total skip counter", skip, getSkipCount(open));
        return this;
    }

    public TaskPartProgressAsserter<RA> assertSuccessCount(int min, int max, boolean open) {
        assertBetween(getSuccessCount(open), min, max, "Total success counter");
        return this;
    }

    public TaskPartProgressAsserter<RA> assertFailureCount(int failure, boolean open) {
        assertEquals("Wrong value of total failure counter", failure, getFailureCount(open));
        return this;
    }

    public TaskPartProgressAsserter<RA> assertFailureCount(int min, int max, boolean open) {
        assertBetween(getFailureCount(open), min, max, "Total failure counter");
        return this;
    }

    private void assertBetween(int actual, int min, int max, String label) {
        if (actual < min) {
            fail(label + " (" + actual + ") is less than minimum expected (" + min + ")");
        } else if (actual > max) {
            fail(label + " (" + actual + ") is more than maximum expected (" + max + ")");
        }
    }

    public TaskPartProgressAsserter<RA> assertComplete() {
        assertThat(information.isComplete()).as("complete").isTrue();
        return this;
    }

    public TaskPartProgressAsserter<RA> assertNotComplete() {
        assertThat(information.isComplete()).as("complete").isNotEqualTo(Boolean.TRUE);
        return this;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public TaskPartProgressAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(information));
        return this;
    }

    private int getSuccessCount(boolean open) {
        return TaskProgressUtil.getProgressForOutcome(information, ItemProcessingOutcomeType.SUCCESS, open);
    }

    private int getFailureCount(boolean open) {
        return TaskProgressUtil.getProgressForOutcome(information, ItemProcessingOutcomeType.FAILURE, open);
    }

    private int getSkipCount(boolean open) {
        return TaskProgressUtil.getProgressForOutcome(information, ItemProcessingOutcomeType.SKIP, open);
    }

    private int getNonFailureCount(boolean open) {
        return getSuccessCount(open) + getSkipCount(open);
    }
}
