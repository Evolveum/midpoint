/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

public class ActivityPolicyRulesContext extends PolicyRulesContext<EvaluatedActivityPolicyRule> {

    /** Values needed for evaluation of policy rules that existed before the current activity was started. */
    private PreexistingValues preexistingValues;

    PreexistingValues getPreexistingValues() {
        return preexistingValues;
    }

    void setPreexistingValues(PreexistingValues preexistingValues) {
        this.preexistingValues = preexistingValues;
    }
}
