/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.*;

import org.jetbrains.annotations.NotNull;

/**
 * Base class for policy rules context.
 * Stores evaluated policy rules. Later it can be extended to store additional information.
 */
class PolicyRulesContext<T extends EvaluatedPolicyRule> {

    private final @NotNull List<T> policyRules = new ArrayList<>();

    public Collection<T> getPolicyRules() {
        return Collections.unmodifiableList(policyRules);
    }

    public T getPolicyRule(@NotNull String ruleId) {
        return policyRules.stream()
                .filter(rule -> Objects.equals(ruleId, rule.getRuleIdentifier().toString()))
                .findFirst()
                .orElse(null);
    }

    public void setPolicyRules(@NotNull List<T> policyRules) {
        this.policyRules.clear();
        this.policyRules.addAll(policyRules);
    }
}
