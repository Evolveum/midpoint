/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil.*;

import com.evolveum.midpoint.schema.statistics.ActivitySynchronizationStatisticsUtil;
import com.evolveum.midpoint.schema.util.SyncSituationUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitySynchronizationStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationTransitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 *  Asserter that checks task synchronization information.
 */
public class SynchronizationInfoAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivitySynchronizationStatisticsType information;

    SynchronizationInfoAsserter(ActivitySynchronizationStatisticsType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public SynchronizationInfoAsserter<RA> assertTransition(SynchronizationSituationType onProcessingStart,
            SynchronizationSituationType onSyncStart, SynchronizationSituationType onSyncEnd,
            SynchronizationExclusionReasonType exclusionReason, int success, int error, int skip) {
        SynchronizationSituationTransitionType matching = SyncSituationUtil.findMatchingTransition(information, onProcessingStart,
                onSyncStart, onSyncEnd, exclusionReason);
        String transition = onProcessingStart + "->" + onSyncStart + "->" + onSyncEnd + " (" + exclusionReason + ")";
        if (matching == null) {
            if (success != 0 || error != 0 || skip != 0) {
                fail("Expected transition for " + transition + " was not found in " + information);
            }
        } else {
            assertThat(getSuccessCount(matching.getCounter()))
                    .as("Expected success count for " + transition)
                    .isEqualTo(success);
            assertThat(getFailureCount(matching.getCounter()))
                    .as("Expected failure count for " + transition)
                    .isEqualTo(error);
            assertThat(getSkipCount(matching.getCounter()))
                    .as("Expected skip count for " + transition)
                    .isEqualTo(skip);
        }
        return this;
    }

    public SynchronizationInfoAsserter<RA> assertTransitions(int count) {
        assertThat(information.getTransition()).hasSize(count);
        return this;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public SynchronizationInfoAsserter<RA> display() {
        IntegrationTestTools.display(desc(), ActivitySynchronizationStatisticsUtil.format(information));
        return this;
    }
}
