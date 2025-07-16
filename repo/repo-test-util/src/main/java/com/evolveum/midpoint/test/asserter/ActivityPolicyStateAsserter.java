/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ActivityPolicyStateAsserter<RA> extends AbstractAsserter<RA> {

    private ActivityPolicyStateType state;

    public ActivityPolicyStateAsserter(ActivityPolicyStateType state, RA returnAsserter, String details) {
        super(returnAsserter, details);

        this.state = state;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityPolicyStateAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(state));
        return this;
    }

    public ActivityPolicyStateAsserter<RA> assertTriggerCount(int expectedCount) {
        List<EvaluatedActivityPolicyTriggerType> triggers = state.getTrigger();

        Assertions.assertThat(triggers).hasSize(expectedCount)
                .withFailMessage("Expected %d triggers, but found %d: %s", expectedCount, triggers.size(), triggers);

        return this;
    }

    public static ActivityPolicyType forName(@NotNull ActivityDefinitionType activityDef, String name) {
        ActivityPoliciesType policies = activityDef.getPolicies();

        Assertions.assertThat(policies)
                .isNotNull();

        return policies.getPolicy().stream()
                .filter(p -> p.getName() != null && p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No activity policy with name '" + name + "' found"));
    }
}
