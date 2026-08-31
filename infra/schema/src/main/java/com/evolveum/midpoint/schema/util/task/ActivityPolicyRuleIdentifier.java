/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util.task;

import java.io.Serializable;

import com.evolveum.midpoint.schema.policy.PlainPolicyRuleIdentifier;
import com.evolveum.midpoint.schema.policy.PolicyRuleIdentifier;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * Identifies a particular policy rule in the activity tree, directly attached.
 *
 * The class was introduced to clearly distinguish rule IDs from other kinds of {@link String} in the code.
 *
 * Note that the current format does not support converting a {@link String} back into {@link ActivityPolicyRuleIdentifier},
 * because we have no way how to parse {@link ActivityPath} from string representation.
 *
 * Other policy rules, including those referenced by `policyRef` and virtual assignments, are identified using
 * {@link PlainPolicyRuleIdentifier}, usually in the form of "object OID : container ID".
 */
public record ActivityPolicyRuleIdentifier(
        @NotNull ActivityPath path,
        long policyId) implements Serializable, PolicyRuleIdentifier {

    public static @NotNull ActivityPolicyRuleIdentifier of(
            @NotNull PolicyRuleType policy, @NotNull ActivityPath path) {
        return new ActivityPolicyRuleIdentifier(path, policy.getId());
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public @NotNull String asString() {
        return path + ":" + policyId;
    }
}
