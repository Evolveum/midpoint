/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.statistics.OutcomeKeyedCounterTypeUtil.*;

import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.schema.util.SyncSituationUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationTransitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 *  Asserter that checks task synchronization information.
 */
public class SynchronizationInfoAsserter<RA> extends AbstractAsserter<RA> {

    private final SynchronizationInformationType information;

    SynchronizationInfoAsserter(SynchronizationInformationType information, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.information = information;
    }

    public SynchronizationInfoAsserter<RA> assertTransition(SynchronizationSituationType onProcessingStart,
            SynchronizationSituationType onSyncStart, SynchronizationSituationType onSyncEnd,
            SynchronizationExclusionReasonType exclusionReason, int success, int error, int skip) {
        SynchronizationSituationTransitionType matching = SyncSituationUtil.findMatchingTransition(information, onProcessingStart,
                onSyncStart, onSyncEnd, exclusionReason);
        if (matching == null) {
            if (success != 0 || error != 0 || skip != 0) {
                fail("Expected transition for " + onProcessingStart + "->" + onSyncStart + "->" + onSyncEnd + " (" +
                        exclusionReason + ") was not found in " + information);
            }
        } else {
            assertThat(getSuccessCount(matching.getCounter())).isEqualTo(success);
            assertThat(getFailureCount(matching.getCounter())).isEqualTo(error);
            assertThat(getSkipCount(matching.getCounter())).isEqualTo(skip);
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
        IntegrationTestTools.display(desc(), SynchronizationInformation.format(information));
        return this;
    }
}
