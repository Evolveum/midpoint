/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import java.util.List;
import java.util.Objects;

import org.assertj.core.api.Assertions;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyStateType;

public class ActivityPoliciesStateAsserter<RA> extends AbstractAsserter<RA> {

    private final ActivityPoliciesStateType state;

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

    public ActivityPolicyStateAsserter<ActivityPoliciesStateAsserter<RA>> activityPolicyState(String policyName) {
        return activityPolicyState(policyName, false);
    }

    public ActivityPolicyStateAsserter<ActivityPoliciesStateAsserter<RA>> activityPolicyState(String policyName, boolean exact) {
        ActivityPolicyStateType policyState = state.getPolicy().stream()
                .filter(p ->
                        exact ?
                                Objects.equals(p.getName(), policyName) :
                                p.getIdentifier() != null && p.getName().contains(policyName))
                .findFirst()
                .orElse(null);

        return policyState != null ? new ActivityPolicyStateAsserter<>(policyState, this, null) : null;
    }

    public ActivityPoliciesStateAsserter<RA> assertPolicyStateCount(int expectedCount) {
        List<ActivityPolicyStateType> policyStates = state.getPolicy();

        Assertions.assertThat(policyStates).hasSize(expectedCount);

        return this;
    }

    public ActivityPoliciesStateAsserter<RA> assertOnePolicyStateTriggers(String identifier, int expectedCount) {
        List<ActivityPolicyStateType> policyStates = state.getPolicy();

        Assertions.assertThat(policyStates).hasSize(1);
        ActivityPolicyStateType policyState = policyStates.get(0);

        Assertions.assertThat(policyState.getIdentifier()).isEqualTo(identifier);
        Assertions.assertThat(policyState.getTrigger()).hasSize(expectedCount);

        return this;
    }
}
