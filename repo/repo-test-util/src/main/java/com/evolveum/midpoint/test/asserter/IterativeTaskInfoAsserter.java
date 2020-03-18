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
public class IterativeTaskInfoAsserter<RA> extends AbstractAsserter<RA> {

    private final IterativeTaskInformationType information;

    IterativeTaskInfoAsserter(IterativeTaskInformationType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public IterativeTaskInfoAsserter<RA> assertTotalCounts(int success, int failure) {
        assertEquals("Wrong value of total success counter", success, information.getTotalSuccessCount());
        assertEquals("Wrong value of total failure counter", failure, information.getTotalFailureCount());
        return this;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public IterativeTaskInfoAsserter display() {
        IntegrationTestTools.display(desc(), IterativeTaskInformation.format(information));
        return this;
    }
}
