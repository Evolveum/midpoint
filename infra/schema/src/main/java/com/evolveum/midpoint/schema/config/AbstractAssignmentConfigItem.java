/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/** Exception from naming convention (because of assignment vs inducement dichotomy). */
public class AbstractAssignmentConfigItem extends ConfigurationItem<AssignmentType> {

    @SuppressWarnings("unused") // called dynamically
    public AbstractAssignmentConfigItem(@NotNull ConfigurationItem<AssignmentType> original) {
        super(original);
    }

    public AbstractAssignmentConfigItem(@NotNull AssignmentType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static AbstractAssignmentConfigItem of(@NotNull AssignmentType bean, @NotNull ConfigurationItemOrigin origin) {
        return new AbstractAssignmentConfigItem(bean, origin);
    }

    public static AbstractAssignmentConfigItem of(
            @NotNull AssignmentType bean,
            @NotNull OriginProvider<? super AssignmentType> originProvider) {
        return new AbstractAssignmentConfigItem(bean, originProvider.origin(bean));
    }

    @Override
    public @NotNull String localDescription() {
        return "assignment/inducement";
    }

    public @Nullable PolicyRuleConfigItem getPolicyRule() {
        return as(
                child(value().getPolicyRule(), AssignmentType.F_POLICY_RULE),
                PolicyRuleConfigItem.class);
    }
}
