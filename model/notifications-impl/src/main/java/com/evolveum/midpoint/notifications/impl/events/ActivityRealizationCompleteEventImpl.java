/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
