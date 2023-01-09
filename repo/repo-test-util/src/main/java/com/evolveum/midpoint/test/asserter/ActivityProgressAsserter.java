/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import static org.assertj.core.api.Assertions.assertThat;

import com.evolveum.midpoint.schema.util.task.ActivityProgressUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityProgressType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingOutcomeType;

/**
 * Asserter that checks raw activity progress data.
 *
 * For checking "processed" progress information see {@link ActivityProgressInformationAsserter}.
 */
@SuppressWarnings("WeakerAccess")
public class ActivityProgressAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivityProgressType information;

    ActivityProgressAsserter(ActivityProgressType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public ActivityProgressAsserter<RA> assertUncommitted(int success, int failure, int skip) {
        assertSuccessCount(success, true);
        assertFailureCount(failure, true);
        assertSkipCount(skip, true);
        return this;
    }

    public ActivityProgressAsserter<RA> assertCommitted(int success, int failure, int skip) {
        assertSuccessCount(success, false);
        assertFailureCount(failure, false);
        assertSkipCount(skip, false);
        return this;
    }

    public ActivityProgressAsserter<RA> assertNoCommitted() {
        return assertCommitted(0, 0, 0);
    }

    public ActivityProgressAsserter<RA> assertNoUncommitted() {
        return assertUncommitted(0, 0, 0);
    }

    public ActivityProgressAsserter<RA> assertSuccessCount(int success, boolean uncommitted) {
        assertEquals("Wrong value of total success counter (uncommitted=" + uncommitted + ")",
                success, getSuccessCount(uncommitted));
        return this;
    }

    public ActivityProgressAsserter<RA> assertSuccessCount(int uncommitted, int committed) {
        assertSuccessCount(uncommitted, true);
        assertSuccessCount(committed, false);
        return this;
    }

    public ActivityProgressAsserter<RA> assertSuccessCount(int min, int max, boolean uncommitted) {
        assertMinMax("Total success counter", min, max, getSuccessCount(uncommitted));
        return this;
    }

    public ActivityProgressAsserter<RA> assertSkipCount(int skip, boolean open) {
        assertEquals("Wrong value of total skip counter", skip, getSkipCount(open));
        return this;
    }

    public ActivityProgressAsserter<RA> assertFailureCount(int failure, boolean uncommitted) {
        assertEquals("Wrong value of total failure counter", failure, getFailureCount(uncommitted));
        return this;
    }

    public ActivityProgressAsserter<RA> assertFailureCount(int min, int max, boolean uncommitted) {
        assertMinMax("Total failure counter", min, max, getFailureCount(uncommitted));
        return this;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityProgressAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(information));
        return this;
    }

    private int getSuccessCount(boolean open) {
        return ActivityProgressUtil.getProgressForOutcome(information, ItemProcessingOutcomeType.SUCCESS, open);
    }

    private int getFailureCount(boolean open) {
        return ActivityProgressUtil.getProgressForOutcome(information, ItemProcessingOutcomeType.FAILURE, open);
    }

    private int getSkipCount(boolean open) {
        return ActivityProgressUtil.getProgressForOutcome(information, ItemProcessingOutcomeType.SKIP, open);
    }

    private int getNonFailureCount(boolean open) {
        return getSuccessCount(open) + getSkipCount(open);
    }

    public ActivityProgressAsserter<RA> assertExpectedTotal(int expected) {
        assertThat(information.getExpectedTotal()).as("expected total").isEqualTo(expected);
        return this;
    }
}
