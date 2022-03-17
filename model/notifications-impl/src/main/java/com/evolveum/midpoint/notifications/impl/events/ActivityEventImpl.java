/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.notifications.api.events.ActivityEvent;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;

@Experimental
public class ActivityEventImpl extends BaseEventImpl implements ActivityEvent {

    @NotNull private final AbstractActivityRun<?, ?, ?> activityRun;

    ActivityEventImpl(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        this.activityRun = activityRun;
    }

    @Override
    public @NotNull AbstractActivityRun<?, ?, ?> getActivityRun() {
        return activityRun;
    }

    @Override
    public @NotNull RunningTask getRunningTask() {
        return activityRun.getRunningTask();
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatus) {
        return false; // This kind of filtering is currently not supported here.
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperation) {
        return false; // This kind of filtering is currently not supported here.
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return eventCategory == EventCategoryType.ACTIVITY_EVENT;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelToString(sb, "activityRun", activityRun, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return toStringPrefix() +
                ", activityRun=" + activityRun +
                '}';
    }
}
