/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.policy.PolicyRuleIdentifier;

/**
 * TODO better name, since this identifier is used for policies loaded from system configuration (global) or task assignments. [viliam]
 */
public record GenericPolicyRuleIdentifier(String ruleId) implements Serializable, PolicyRuleIdentifier {

    /**
     * Rule id doesn't have specific format, however currently OID:ID is used where OID
     * is source object of the policy rule, ID is a container identifier.
     */
    @NotNull
    public static GenericPolicyRuleIdentifier of(String ruleId) {
        return new GenericPolicyRuleIdentifier(ruleId);
    }

    @Override
    public String asString() {
        return ruleId;
    }
}
