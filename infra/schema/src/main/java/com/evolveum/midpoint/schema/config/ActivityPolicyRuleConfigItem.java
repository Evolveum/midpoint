/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

public class ActivityPolicyRuleConfigItem extends AbstractPolicyRuleConfigItem<PolicyRuleType> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public ActivityPolicyRuleConfigItem(@NotNull ConfigurationItem<PolicyRuleType> original) {
        super(original);
    }

    @Override
    public @NotNull String localDescription() {
        String name = value().getName();
        return "activity policy rule " + (name != null ? "'%s'".formatted(name) : "(without name)");
    }

    @Override
    public ActivityPolicyRuleConfigItem clone() {
        return new ActivityPolicyRuleConfigItem(super.clone());
    }

}
