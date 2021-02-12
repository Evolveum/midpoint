/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;

import com.evolveum.midpoint.test.IntegrationTestTools;

/**
 *  Asserter that checks iterative task information.
 */
@SuppressWarnings("WeakerAccess")
public class IterativeTaskInfoAsserter<RA> extends AbstractAsserter<RA> {

    private final IterativeTaskInformationType information;

    IterativeTaskInfoAsserter(IterativeTaskInformationType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public IterativeTaskInfoAsserter<RA> assertTotalCounts(int success, int failure) {
        assertSuccessCount(success);
        assertFailureCount(failure);
        return this;
    }

    public IterativeTaskInfoAsserter<RA> assertSuccessCount(int success) {
        assertEquals("Wrong value of total success counter", success, information.getTotalSuccessCount());
        return this;
    }

    public IterativeTaskInfoAsserter<RA> assertSuccessCount(int min, int max) {
        assertBetween(information.getTotalSuccessCount(), min, max, "Total success counter");
        return this;
    }

    public IterativeTaskInfoAsserter<RA> assertFailureCount(int failure) {
        assertEquals("Wrong value of total failure counter", failure, information.getTotalFailureCount());
        return this;
    }

    public IterativeTaskInfoAsserter<RA> assertFailureCount(int min, int max) {
        assertBetween(information.getTotalFailureCount(), min, max, "Total failure counter");
        return this;
    }

    public IterativeTaskInfoAsserter<RA> assertLastFailureObjectName(String expected) {
        assertEquals("Wrong 'last failure' object name", expected, information.getLastFailureObjectName());
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

    public IterativeTaskInfoAsserter<RA> display() {
        IntegrationTestTools.display(desc(), IterativeTaskInformation.format(information));
        return this;
    }
}
