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

    @SuppressWarnings("unused") // called dynamically
    public PolicyRuleConfigItem(@NotNull ConfigurationItem<PolicyRuleType> original) {
        super(original);
    }

    public PolicyRuleConfigItem(@NotNull PolicyRuleType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static PolicyRuleConfigItem of(@NotNull PolicyRuleType bean, @NotNull ConfigurationItemOrigin origin) {
        return new PolicyRuleConfigItem(bean, origin);
    }

    public static PolicyRuleConfigItem of(
            @NotNull PolicyRuleType bean,
            @NotNull OriginProvider<? super PolicyRuleType> originProvider) {
        return new PolicyRuleConfigItem(bean, originProvider.origin(bean));
    }

    @Override
    public @NotNull String localDescription() {
        return "policy rule";
    }

    @Override
    public PolicyRuleConfigItem clone() {
        return new PolicyRuleConfigItem(super.clone());
    }

}
