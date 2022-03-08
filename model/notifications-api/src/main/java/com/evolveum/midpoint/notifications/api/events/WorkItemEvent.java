/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.schema.util.WorkItemId;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * An event bound to specific {@link CaseWorkItemType}.
 */
public interface WorkItemEvent {

    SimpleObjectRef getAssignee();

    /**
     * An URL where this work item can be completed. (Points to midPoint GUI.)
     *
     * Returns null if such a link cannot be created.
     */
    @Nullable String getWorkItemUrl();

    /**
     * ID of the work item. We are not able to generate ID right from the work item, because it's not
     * attached to its case yet.
     */
    @NotNull WorkItemId getWorkItemId();
}
