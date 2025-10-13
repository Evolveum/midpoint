/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.events;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.notifications.api.events.ActivityRealizationCompleteEvent;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

public class ActivityRealizationCompleteEventImpl extends ActivityEventImpl implements ActivityRealizationCompleteEvent {

    public ActivityRealizationCompleteEventImpl(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        super(activityRun);
    }
}
