/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.*;

import org.jetbrains.annotations.NotNull;

public class ActivityPolicyRulesContext {

    /** Values needed for evaluation of policy rules that existed before the current activity was started. */
    private PreexistingValues preexistingValues;

    private final @NotNull List<ActivityPolicyRule> policyRules = new ArrayList<>();

    public Collection<ActivityPolicyRule> getPolicyRules() {
        return Collections.unmodifiableList(policyRules);
    }

    public ActivityPolicyRule getPolicyRule(@NotNull String ruleId) {
        return policyRules.stream()
                .filter(rule -> Objects.equals(ruleId, rule.getRuleIdentifier().toString()))
                .findFirst()
                .orElse(null);
    }

    public void setPolicyRules(@NotNull List<ActivityPolicyRule> policyRules) {
        this.policyRules.clear();
        this.policyRules.addAll(policyRules);
    }

    PreexistingValues getPreexistingValues() {
        return preexistingValues;
    }

    void setPreexistingValues(PreexistingValues preexistingValues) {
        this.preexistingValues = preexistingValues;
    }
}
