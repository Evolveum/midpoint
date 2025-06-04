/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class ActivityPolicyRulesContext {

    private @NotNull List<EvaluatedActivityPolicyRule> policyRules = new ArrayList<>();

    public @NotNull List<EvaluatedActivityPolicyRule> getPolicyRules() {
        return policyRules;
    }

    public void setPolicyRules(@NotNull List<EvaluatedActivityPolicyRule> policyRules) {
        this.policyRules.clear();
        this.policyRules.addAll(policyRules);
    }

    public EvaluatedActivityPolicyRule getPolicyRule(@NotNull String ruleId) {
        return policyRules.stream()
                .filter(rule -> Objects.equals(ruleId, rule.getRuleId()))
                .findFirst()
                .orElse(null);
    }
}
