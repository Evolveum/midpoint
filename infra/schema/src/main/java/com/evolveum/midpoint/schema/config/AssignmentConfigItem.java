/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.jetbrains.annotations.NotNull;

public class AssignmentConfigItem extends AbstractAssignmentConfigItem {

    public AssignmentConfigItem(@NotNull ConfigurationItem<AssignmentType> original) {
        super(original);
    }

    public AssignmentConfigItem(@NotNull AssignmentType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static AbstractAssignmentConfigItem of(
            @NotNull AssignmentType bean,
            @NotNull OriginProvider<? super AssignmentType> originProvider) {
        return new AssignmentConfigItem(bean, originProvider.origin(bean));
    }

    @Override
    public @NotNull String localDescription() {
        return "assignment";
    }
}
