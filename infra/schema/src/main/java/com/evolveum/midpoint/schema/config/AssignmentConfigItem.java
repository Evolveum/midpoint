/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.jetbrains.annotations.NotNull;

public class AssignmentConfigItem extends AbstractAssignmentConfigItem {

    @SuppressWarnings("unused") // invoked dynamically
    public AssignmentConfigItem(@NotNull ConfigurationItem<AssignmentType> original) {
        super(original);
    }

    private AssignmentConfigItem(@NotNull AssignmentType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin, null);
    }

    public static AssignmentConfigItem of(
            @NotNull AssignmentType bean,
            @NotNull OriginProvider<? super AssignmentType> originProvider) {
        return new AssignmentConfigItem(bean, originProvider.origin(bean));
    }

    @Override
    public @NotNull String localDescription() {
        return "assignment";
    }
}
