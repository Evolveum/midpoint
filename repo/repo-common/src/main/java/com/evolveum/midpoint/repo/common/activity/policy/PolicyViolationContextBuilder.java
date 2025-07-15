/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.task.api.PolicyViolationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyActionType;

public class PolicyViolationContextBuilder {

    public static PolicyViolationContext from(
            EvaluatedPolicyReaction reaction, ActivityPolicyActionType action, Integer executionAttempt) {

        EvaluatedActivityPolicyRule rule = reaction.getRule();

        return new PolicyViolationContext(
                rule.getRuleIdentifier(), rule.getName(), reaction.getName(), action, executionAttempt);
    }
}
