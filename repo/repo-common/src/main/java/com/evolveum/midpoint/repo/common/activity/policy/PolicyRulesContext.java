/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.*;

import org.jetbrains.annotations.NotNull;

/**
 * Base class for policy rules context.
 * Stores policy rules and counters for each rule.
 */
public class PolicyRulesContext<T extends EvaluatedPolicyRule> {

    private final @NotNull List<T> policyRules = new ArrayList<>();

    public Collection<T> getPolicyRules() {
        return Collections.unmodifiableList(policyRules);
    }

    public T getPolicyRule(@NotNull String ruleId) {
        return policyRules.stream()
                .filter(rule -> Objects.equals(ruleId, rule.getRuleIdentifier()))
                .findFirst()
                .orElse(null);
    }

    public void setPolicyRules(@NotNull List<T> policyRules) {
        this.policyRules.clear();
        this.policyRules.addAll(policyRules);
    }
}
