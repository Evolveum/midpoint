/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;

/**
 * Decision about access to something. Used as an output of authorization processing code. But may be also used
 * for other things, such as decisions to access classes and methods in sandboxes.
 *
 * @author Radovan Semancik
 */
public enum AccessDecision {

    /**
     * Access explicitly allowed.
     */
    ALLOW(AuthorizationDecisionType.ALLOW),

    /**
     * Access explicitly denied.
     */
    DENY(AuthorizationDecisionType.DENY),

    /**
     * Means "no decision" or "not allowed yet".
     */
    DEFAULT(null);

    private final AuthorizationDecisionType authorizationDecisionType;

    AccessDecision(AuthorizationDecisionType authorizationDecisionType) {
        this.authorizationDecisionType = authorizationDecisionType;
    }

    public AuthorizationDecisionType getAuthorizationDecisionType() {
        return authorizationDecisionType;
    }

    /**
     * Rules:
     *
     * - `null` values are ignored,
     * - any {@link AccessDecision#DENY} means {@link AccessDecision#DENY},
     * - any {@link AccessDecision#DEFAULT} means {@link AccessDecision#DEFAULT},
     * - otherwise, only {@link AccessDecision#ALLOW} remains, leading to {@link AccessDecision#ALLOW}.
     *
     * This also means that this operation is associative.
     */
    public static AccessDecision combine(AccessDecision oldDecision, AccessDecision newDecision) {
        if (oldDecision == null) {
            return newDecision;
        }
        if (newDecision == null) {
            return oldDecision;
        }
        if (oldDecision == DENY || newDecision == DENY) {
            return DENY;
        }
        if (oldDecision == DEFAULT || newDecision == DEFAULT) {
            return DEFAULT;
        }
        if (oldDecision == ALLOW || newDecision == ALLOW) {
            return ALLOW;
        }
        throw new IllegalStateException("Unexpected combine " + oldDecision + "+" + newDecision);
    }

    public static AccessDecision translate(AuthorizationDecisionType authorizationDecisionType) {
        if (authorizationDecisionType == null) {
            return AccessDecision.DEFAULT;
        }
        switch (authorizationDecisionType) {
            case ALLOW:
                return ALLOW;
            case DENY:
                return DENY;
            default:
                throw new IllegalStateException("Unknown AuthorizationDecisionType " + authorizationDecisionType);
        }
    }
}
