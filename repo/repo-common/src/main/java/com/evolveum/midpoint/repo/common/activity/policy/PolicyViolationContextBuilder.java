/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.repo.common.activity.PolicyViolationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyActionType;

public class PolicyViolationContextBuilder {

    public static PolicyViolationContext from(
            EvaluatedPolicyReaction reaction, ActivityPolicyActionType action, Integer executionAttempt) {

        EvaluatedActivityPolicyRule rule = reaction.getRule();

        return new PolicyViolationContext(
                rule.getRuleIdentifier(), rule.getName(), reaction.getName(), action, executionAttempt);
    }
}
