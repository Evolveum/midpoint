/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GlobalPolicyRuleConfigItem extends ConfigurationItem<GlobalPolicyRuleType> {

    public GlobalPolicyRuleConfigItem(@NotNull GlobalPolicyRuleType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static GlobalPolicyRuleConfigItem of(@NotNull GlobalPolicyRuleType bean, @NotNull ConfigurationItemOrigin origin) {
        return new GlobalPolicyRuleConfigItem(bean, origin);
    }

    public static GlobalPolicyRuleConfigItem embedded(@NotNull GlobalPolicyRuleType bean) {
        return new GlobalPolicyRuleConfigItem(bean, ConfigurationItemOrigin.embedded(bean));
    }

    public @Nullable String getName() {
        return value().getName();
    }
}
