/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import java.util.List;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyStateType;

import org.assertj.core.api.Assertions;

public class ActivityPoliciesStateAsserter<RA> extends AbstractAsserter<RA> {

    private ActivityPoliciesStateType state;

    public ActivityPoliciesStateAsserter(ActivityPoliciesStateType state, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.state = state;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityPoliciesStateAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(state));
        return this;
    }

    public ActivityPoliciesStateAsserter<RA> assertOnePolicyStateTriggers(String identifier, int expectedCount) {
        List<ActivityPolicyStateType> policyStates = state.getActivityPolicies();

        Assertions.assertThat(policyStates).hasSize(1);
        ActivityPolicyStateType policyState = policyStates.get(0);

        Assertions.assertThat(policyState.getIdentifier()).isEqualTo(identifier);
        Assertions.assertThat(policyState.getTriggers()).hasSize(expectedCount);

        return this;
    }
}
