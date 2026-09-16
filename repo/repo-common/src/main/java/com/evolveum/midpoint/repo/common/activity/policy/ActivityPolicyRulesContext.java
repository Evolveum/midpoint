/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.*;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

import org.jetbrains.annotations.NotNull;

/**
 * Part of {@link AbstractActivityRun} that describes everything needed for evaluation of activity policy rules.
 *
 * Don't confuse this with {@link ActivityPolicyRuleEvaluationContext}, which is a context for evaluation of a single rule.
 *
 * @see AbstractActivityRun#activityPolicyRulesContext
 */
public class ActivityPolicyRulesContext {

    /** Values needed for evaluation of policy rules that existed before the current activity was started. */
    private PreexistingValues preexistingValues;

    /** Collected policy rules. They are all enabled (regarding {@link PolicyRuleType#isEnabled()}). */
    private final @NotNull List<ActivityPolicyRule> policyRules = new ArrayList<>();

    public Collection<ActivityPolicyRule> getPolicyRules() {
        return Collections.unmodifiableList(policyRules);
    }

    public void setPolicyRules(@NotNull List<ActivityPolicyRule> policyRules) {
        this.policyRules.clear(); // probably not needed (we should set policy rules only once for each run), but just to be sure
        this.policyRules.addAll(policyRules);
    }

    PreexistingValues getPreexistingValues() {
        return preexistingValues;
    }

    void setPreexistingValues(PreexistingValues preexistingValues) {
        this.preexistingValues = preexistingValues;
    }
}
