/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityCounterGroupsType;

import org.jetbrains.annotations.NotNull;

public enum ActivityCountersGroup {

    POLICY_RULES(ActivityCounterGroupsType.F_POLICY_RULES);

    @NotNull private final ItemName itemName;

    ActivityCountersGroup(@NotNull ItemName itemName) {
        this.itemName = itemName;
    }

    public @NotNull ItemName getItemName() {
        return itemName;
    }
}
