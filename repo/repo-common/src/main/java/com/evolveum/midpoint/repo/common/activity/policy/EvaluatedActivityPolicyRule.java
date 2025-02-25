/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionType;

public class EvaluatedActivityPolicyRule {

    private final ActivityPolicyType policy;

    private final List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = new ArrayList<>();

    public EvaluatedActivityPolicyRule(ActivityPolicyType policy) {
        this.policy = policy;
    }

    public ActivityPolicyType getPolicy() {
        return policy;
    }

    public boolean containsAction(Class<? extends PolicyActionType> policyActionType) {
        return getActions(policy.getPolicyActions()).stream()
                .anyMatch(policyActionType::isInstance);
    }

    private List<PolicyActionType> getActions(ActivityPolicyActionsType actions) {
        if (actions == null) {
            return List.of();
        }

        List<PolicyActionType> result = new ArrayList<>();

        addAction(result, actions.getNotification());
        addAction(result, actions.getSuspendTask());

        return result;
    }

    private void addAction(List<PolicyActionType> actions, PolicyActionType action) {
        if (action != null) {
            actions.add(action);
        }
    }

    @NotNull
    public List<EvaluatedActivityPolicyRuleTrigger<?>> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<EvaluatedActivityPolicyRuleTrigger<?>> triggers) {
        this.triggers.clear();

        if (triggers != null) {
            this.triggers.addAll(triggers);
        }
    }
}
