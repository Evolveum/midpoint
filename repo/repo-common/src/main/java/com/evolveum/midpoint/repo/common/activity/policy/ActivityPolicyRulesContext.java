/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ActivityPolicyRulesContext {

    private @NotNull List<EvaluatedActivityPolicyRule> policyRules = new ArrayList<>();

    public @NotNull List<EvaluatedActivityPolicyRule> getPolicyRules() {
        return policyRules;
    }

    public void setPolicyRules(@NotNull List<EvaluatedActivityPolicyRule> policyRules) {
        this.policyRules.clear();
        this.policyRules.addAll(policyRules);
    }
}
