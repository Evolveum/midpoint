/*
 * Copyright (c) 2019-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

/**
 * Information about assignment target evaluation that is stored in the evaluated assignment target cache.
 */

class AssignmentTargetEvaluationInformation {

    /**
     * True if the assignment target contributes (provides) some "target" policy rules. This means that we should not
     * cache its evaluation for the whole focus processing, because the policy rules should be applied separately to all
     * the evaluated focus assignments.
     *
     * Therefore, when starting evaluation of next focus assignment, we should remove all existing cache entries that have
     * this value set to true.
     *
     * See also MID-5827.
     *
     * The default of "true" is safer -- it is better to errorneously evict an entry from the cache than to omit some
     * necessary target policy rules.
     */
    private boolean bringsTargetPolicyRules = true;

    void setBringsTargetPolicyRules(boolean bringsTargetPolicyRules) {
        this.bringsTargetPolicyRules = bringsTargetPolicyRules;
    }

    /**
     * @return true if this target evaluation should be cached for single assignment evaluation only.
     *
     * Currently we decide only based on whether it brings any target policy rules.
     */
    boolean isAssignmentScoped() {
        return bringsTargetPolicyRules;
    }
}
