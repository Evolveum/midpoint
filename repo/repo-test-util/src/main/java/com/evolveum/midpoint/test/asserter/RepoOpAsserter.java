/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
