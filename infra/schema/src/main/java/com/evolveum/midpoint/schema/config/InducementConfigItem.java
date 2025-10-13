/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.jetbrains.annotations.NotNull;

public class InducementConfigItem extends AbstractAssignmentConfigItem {

    public InducementConfigItem(@NotNull ConfigurationItem<AssignmentType> original) {
        super(original);
    }
}
