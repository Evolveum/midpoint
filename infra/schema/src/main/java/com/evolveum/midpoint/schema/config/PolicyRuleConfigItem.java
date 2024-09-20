/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import org.jetbrains.annotations.NotNull;

public class PolicyRuleConfigItem extends AbstractPolicyRuleConfigItem<PolicyRuleType> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public PolicyRuleConfigItem(@NotNull ConfigurationItem<PolicyRuleType> original) {
        super(original);
    }

    @Override
    public @NotNull String localDescription() {
        String name = value().getName();
        return "policy rule " + (name != null ? "'%s'".formatted(name) : "(without name)");
    }

    @Override
    public PolicyRuleConfigItem clone() {
        return new PolicyRuleConfigItem(super.clone());
    }

}
