/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.*;

import org.jetbrains.annotations.NotNull;

public class PolicyRulesContext<T extends EvaluatedPolicyRule> {

    private final @NotNull List<T> policyRules = new ArrayList<>();

    private final @NotNull Map<String, Integer> ruleCounters = new HashMap<>();

    public Collection<T> getPolicyRules() {
        return Collections.unmodifiableList(policyRules);
    }

    public T getPolicyRule(@NotNull String ruleId) {
        return policyRules.stream()
                .filter(rule -> Objects.equals(ruleId, rule.getRuleIdentifier()))
                .findFirst()
                .orElse(null);
    }

    public void addPolicyRules(@NotNull List<T> policyRules) {
        this.policyRules.addAll(policyRules);
    }

    public void clearPolicyRules() {
        policyRules.clear();
    }

    public Integer getCounter(@NotNull String ruleId) {
        return ruleCounters.get(ruleId);
    }

    public void setCounter(@NotNull String ruleId, int count) {
        ruleCounters.put(ruleId, count);
    }
}
