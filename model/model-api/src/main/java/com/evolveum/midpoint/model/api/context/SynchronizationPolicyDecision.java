/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationPolicyDecisionType;

/**
 * Describes what the policy "decides" about a specific account.
 *
 * @author Radovan Semancik
 *
 */
public enum SynchronizationPolicyDecision {

    /**
     * New account that is going to be added (and linked)
     */
    ADD,

    /**
     * Existing account that is going to be deleted (and unlinked)
     */
    DELETE,

    /**
     * Existing account that is kept as it is (remains linked).
     * Note: there still may be attribute or entitlement changes.
     */
    KEEP,

    /**
     * Existing account that should be unlinked (but NOT deleted). By unlinking we mean either physically removing
     * a value from `linkRef` (if shadow does not exist any more), or changing the relation from `org:default` to
     * `org:related`.
     */
    UNLINK,

    /**
     * The projection is broken. I.e. there is some (fixable) state that
     * prevents proper operations with the projection. This may be schema violation
     * problem, security problem (access denied), misconfiguration, etc.
     * Broken projections will be kept in the state that they are (maintaining
     * status quo) until the problem is fixed. We will do no operations on broken
     * projections and we will NOT unlink them or delete them.
     */
    BROKEN,

    /**
     * The account is not usable. Context was created, but the account will be skipped.
     * this is used only for evaluation assigment and the assigment policies
     */
    IGNORE;

    public SynchronizationPolicyDecisionType toSynchronizationPolicyDecisionType() {
        switch (this) {
            case ADD: return SynchronizationPolicyDecisionType.ADD;
            case DELETE: return SynchronizationPolicyDecisionType.DELETE;
            case KEEP: return SynchronizationPolicyDecisionType.KEEP;
            case UNLINK: return SynchronizationPolicyDecisionType.UNLINK;
            case BROKEN: return SynchronizationPolicyDecisionType.BROKEN;
            default: throw new AssertionError("Unknown value of SynchronizationPolicyDecision: " + this);
        }
    }

    public static SynchronizationPolicyDecision fromSynchronizationPolicyDecisionType(SynchronizationPolicyDecisionType value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case ADD: return ADD;
            case DELETE: return DELETE;
            case KEEP: return KEEP;
            case UNLINK: return UNLINK;
            case BROKEN: return BROKEN;
            default: throw new AssertionError("Unknown value of SynchronizationPolicyDecisionType: " + value);
        }
    }
}
