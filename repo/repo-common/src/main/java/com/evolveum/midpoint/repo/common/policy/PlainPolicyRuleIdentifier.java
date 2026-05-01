/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

/**
 * Policy rule identifier based on a plain string.
 */
public record PlainPolicyRuleIdentifier(String ruleId) implements Serializable, PolicyRuleIdentifier {

    /**
     * Rule id doesn't have specific format, however currently OID:ID is used where OID
     * is source object of the policy rule, ID is a container identifier.
     */
    @NotNull
    public static PlainPolicyRuleIdentifier of(String ruleId) {
        return new PlainPolicyRuleIdentifier(ruleId);
    }

    @NotNull
    public static PlainPolicyRuleIdentifier of(@NotNull String objectOid, @NotNull Long inducementCID) {
        return of(objectOid + ":" + inducementCID);
    }

    @Override
    public @NotNull String asString() {
        return ruleId;
    }

    @Override
    public @NotNull String toString() {
        return asString();
    }
}
