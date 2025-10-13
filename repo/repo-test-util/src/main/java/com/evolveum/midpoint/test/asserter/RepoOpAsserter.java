/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.schema.statistics.RepositoryPerformanceInformationUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;

import static org.testng.AssertJUnit.assertEquals;

/**
 *  Asserter that checks repository operation counts.
 *  EXPERIMENTAL
 */
public class RepoOpAsserter extends AbstractAsserter<Void> {

    private final PerformanceInformation repoPerformanceInformation;

    public RepoOpAsserter(PerformanceInformation repoPerformanceInformation, String details) {
        super(details);
        this.repoPerformanceInformation = repoPerformanceInformation.clone();       // cloning to gather current state
    }

    public RepoOpAsserter assertOp(String operation, int expected) {
        assertEquals("Wrong number of invocations of " + operation, expected, repoPerformanceInformation.getInvocationCount(operation));
        return this;
    }

    public RepoOpAsserter assertOp(String operation, int expectedMin, int expectedMax) {
        assertMinMax("Wrong number of invocations of " + operation, expectedMin, expectedMax,
                repoPerformanceInformation.getInvocationCount(operation));
        return this;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public RepoOpAsserter display() {
        IntegrationTestTools.display(desc(),
                RepositoryPerformanceInformationUtil.format(repoPerformanceInformation.toRepositoryPerformanceInformationType()));
        return this;
    }
}
