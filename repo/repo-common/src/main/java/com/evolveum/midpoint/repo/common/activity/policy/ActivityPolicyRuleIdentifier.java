/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.schema.util.task.ActivityPath;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Identifies a particular policy rule in the activity tree. The class was introduced to clearly distinguish rule IDs
 * from other kinds of {@link String} in the code.
 *
 * Note that the current format does not support converting a {@link String} back into {@link ActivityPolicyRuleIdentifier},
 * because we have no way how to parse {@link ActivityPath} from string representation.
 */
public record ActivityPolicyRuleIdentifier(
        @NotNull ActivityPath path,
        long policyId) implements Serializable {

    public static @NotNull ActivityPolicyRuleIdentifier of(
            @NotNull ActivityPolicyType policy, @NotNull ActivityPath path) {
        return new ActivityPolicyRuleIdentifier(path, policy.getId());
    }

    @Override
    public String toString() {
        return path + ":" + policyId;
    }
}
