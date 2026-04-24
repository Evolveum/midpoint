/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.policy.PolicyRuleIdentifier;

import org.jspecify.annotations.NonNull;

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

    @Override
    public @NonNull String asString() {
        return ruleId;
    }

    @Override
    public @NotNull String toString() {
        return asString();
    }
}
